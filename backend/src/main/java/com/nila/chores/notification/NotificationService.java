package com.nila.chores.notification;

import com.nila.chores.task.Task;
import com.nila.chores.task.TaskAssignment;
import com.nila.chores.task.TaskAssignmentRepository;
import com.nila.chores.task.TaskRepository;
import com.nila.chores.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Sends per-kid reminder notifications for a given task.
 * Each kid with contact info gets a message on every configured channel they have.
 * Kids with neither email nor telegram_chat_id are skipped (logged).
 * A notification_log row is written for every send attempt.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository assignmentRepository;
    private final NotificationLogRepository logRepository;
    private final EmailSender emailSender;
    private final TelegramSender telegramSender;

    public NotificationService(TaskRepository taskRepository,
                                TaskAssignmentRepository assignmentRepository,
                                NotificationLogRepository logRepository,
                                EmailSender emailSender,
                                TelegramSender telegramSender) {
        this.taskRepository = taskRepository;
        this.assignmentRepository = assignmentRepository;
        this.logRepository = logRepository;
        this.emailSender = emailSender;
        this.telegramSender = telegramSender;
    }

    /**
     * Send a reminder for the given task to every assigned kid with contact info.
     * Returns one {@link ReminderResult} per kid-channel pair (or one per skipped kid).
     *
     * @param taskId task to remind about
     * @return list of per-kid per-channel results
     * @throws ResponseStatusException 404 if task not found
     */
    @Transactional
    public List<ReminderResult> sendReminderForTask(Long taskId) {
        log.info("event=notification.send-reminder.start taskId={}", taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> {
                    log.warn("event=notification.send-reminder.task-not-found taskId={}", taskId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
                });

        List<TaskAssignment> assignments = assignmentRepository.findByTaskId(taskId);
        log.info("event=notification.send-reminder.assignments taskId={} count={}", taskId, assignments.size());

        List<ReminderResult> results = new ArrayList<>();

        for (TaskAssignment assignment : assignments) {
            User kid = assignment.getUser();
            results.addAll(processKid(task, kid));
        }

        log.info("event=notification.send-reminder.complete taskId={} totalResults={}", taskId, results.size());
        return results;
    }

    private List<ReminderResult> processKid(Task task, User kid) {
        String kidName = kid.getDisplayName();
        Long kidId = kid.getId();

        log.info("event=notification.kid.start taskId={} kidId={} kidName={} hasEmail={} hasTelegram={}",
                task.getId(), kidId, kidName,
                kid.getEmail() != null && !kid.getEmail().isBlank(),
                kid.getTelegramChatId() != null);

        boolean hasEmail = kid.getEmail() != null && !kid.getEmail().isBlank();
        boolean hasTelegram = kid.getTelegramChatId() != null;

        if (!hasEmail && !hasTelegram) {
            log.warn("event=notification.kid.skipped taskId={} kidId={} kidName={} reason=no-contact-info",
                    task.getId(), kidId, kidName);
            return List.of(new ReminderResult(kidId, kidName, null, false, true, "no contact info"));
        }

        String subject = "Reminder: " + task.getTitle();
        String body = "Hi " + kidName + ", this is a reminder to complete: " + task.getTitle() + ".";

        List<ReminderResult> results = new ArrayList<>();

        if (hasEmail) {
            results.add(sendViaEmail(task, kid, subject, body));
        }

        if (hasTelegram) {
            results.add(sendViaTelegram(task, kid, subject, body));
        }

        return results;
    }

    private ReminderResult sendViaEmail(Task task, User kid, String subject, String body) {
        log.info("event=notification.email.attempt taskId={} kidId={}", task.getId(), kid.getId());
        SendResult result = emailSender.send(kid.getEmail(), subject, body);

        NotificationLog entry = buildLog(task, kid, "EMAIL", result, body);
        logRepository.save(entry);

        if (result.ok()) {
            log.info("event=notification.email.sent taskId={} kidId={} kidName={}",
                    task.getId(), kid.getId(), kid.getDisplayName());
            return new ReminderResult(kid.getId(), kid.getDisplayName(), "EMAIL", true, false, null);
        } else {
            log.warn("event=notification.email.failed taskId={} kidId={} kidName={} error={}",
                    task.getId(), kid.getId(), kid.getDisplayName(), result.error());
            return new ReminderResult(kid.getId(), kid.getDisplayName(), "EMAIL", false, false, result.error());
        }
    }

    private ReminderResult sendViaTelegram(Task task, User kid, String subject, String body) {
        String chatId = String.valueOf(kid.getTelegramChatId());
        log.info("event=notification.telegram.attempt taskId={} kidId={} chatId={}",
                task.getId(), kid.getId(), chatId);
        SendResult result = telegramSender.send(chatId, subject, body);

        NotificationLog entry = buildLog(task, kid, "TELEGRAM", result, body);
        logRepository.save(entry);

        if (result.ok()) {
            log.info("event=notification.telegram.sent taskId={} kidId={} kidName={} chatId={}",
                    task.getId(), kid.getId(), kid.getDisplayName(), chatId);
            return new ReminderResult(kid.getId(), kid.getDisplayName(), "TELEGRAM", true, false, null);
        } else {
            log.warn("event=notification.telegram.failed taskId={} kidId={} kidName={} chatId={} error={}",
                    task.getId(), kid.getId(), kid.getDisplayName(), chatId, result.error());
            return new ReminderResult(kid.getId(), kid.getDisplayName(), "TELEGRAM", false, false, result.error());
        }
    }

    private NotificationLog buildLog(Task task, User kid, String channel, SendResult result, String body) {
        NotificationLog log = new NotificationLog();
        log.setTask(task);
        log.setUser(kid);
        log.setChannel(channel);
        log.setStatus(result.ok() ? "SENT" : "FAILED");
        log.setError(result.ok() ? null : result.error());
        log.setMessage(body);
        return log;
    }

    /**
     * Per-kid per-channel result DTO.
     *
     * @param kidId   kid's user ID
     * @param kidName kid's display name
     * @param channel "EMAIL", "TELEGRAM", or null when skipped
     * @param sent    true if the send succeeded
     * @param skipped true if the kid had no contact info (skipped entirely)
     * @param error   error message on failure; null on success or skip
     */
    public record ReminderResult(
            Long kidId,
            String kidName,
            String channel,
            boolean sent,
            boolean skipped,
            String error
    ) {}
}
