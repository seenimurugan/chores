package com.nila.chores.notification;

import com.nila.chores.task.Task;
import com.nila.chores.task.TaskAssignment;
import com.nila.chores.task.TaskAssignmentRepository;
import com.nila.chores.task.TaskRepository;
import com.nila.chores.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD unit tests for NotificationService.sendReminderForTask.
 * Channels (EmailSender, TelegramSender) are mocked; only logic is tested.
 * LENIENT strictness: @BeforeEach stubs isConfigured() for all tests,
 * but not every test exercises both senders.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock TaskAssignmentRepository assignmentRepository;
    @Mock NotificationLogRepository logRepository;
    @Mock EmailSender emailSender;
    @Mock TelegramSender telegramSender;

    NotificationService service;

    @BeforeEach
    void setUp() {
        when(emailSender.isConfigured()).thenReturn(true);
        when(telegramSender.isConfigured()).thenReturn(true);
        service = new NotificationService(taskRepository, assignmentRepository, logRepository,
                emailSender, telegramSender);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Task task(Long id, String title) {
        Task t = new Task();
        t.setId(id);
        t.setTitle(title);
        t.setRecurrence(Task.Recurrence.DAILY);
        t.setPoints(1);
        t.setActive(true);
        return t;
    }

    private User kid(Long id, String displayName, String email, Long telegramChatId) {
        User u = new User();
        u.setId(id);
        u.setUsername("kid" + id);
        u.setDisplayName(displayName);
        u.setRole(User.Role.KID);
        u.setEmail(email);
        u.setTelegramChatId(telegramChatId);
        u.setTimezone("Europe/London");
        u.setAvatarColor("#4263eb");
        u.setPasswordHash("hash");
        return u;
    }

    private TaskAssignment assignment(Task task, User user) {
        TaskAssignment a = new TaskAssignment();
        a.setTask(task);
        a.setUser(user);
        return a;
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    /**
     * Kid with both email + telegram: both channels attempted, 2 log rows written.
     */
    @Test
    void kidWithEmailAndTelegram_bothChannelsAttempted_twoLogRowsWritten() {
        Task t = task(1L, "Take out bins");
        User kid = kid(10L, "Ammu", "ammu@example.com", -5139466273L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
        when(assignmentRepository.findByTaskId(1L)).thenReturn(List.of(assignment(t, kid)));
        when(emailSender.send(eq("ammu@example.com"), anyString(), anyString()))
                .thenReturn(SendResult.success());
        when(telegramSender.send(eq("-5139466273"), anyString(), anyString()))
                .thenReturn(SendResult.success());

        List<NotificationService.ReminderResult> results = service.sendReminderForTask(1L);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(NotificationService.ReminderResult::sent);

        // 2 notification_log rows saved (email + telegram)
        var captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(2)).save(captor.capture());
        List<NotificationLog> logs = captor.getAllValues();
        assertThat(logs).extracting(NotificationLog::getChannel)
                .containsExactlyInAnyOrder("EMAIL", "TELEGRAM");
        assertThat(logs).allMatch(l -> "SENT".equals(l.getStatus()));
    }

    /**
     * Kid with email only: email attempted, telegram skipped, 1 log row written.
     */
    @Test
    void kidWithEmailOnly_emailAttempted_telegramSkipped_oneLogRow() {
        Task t = task(2L, "Homework");
        User kid = kid(11L, "Kuttima", "kuttima@example.com", null);
        when(taskRepository.findById(2L)).thenReturn(Optional.of(t));
        when(assignmentRepository.findByTaskId(2L)).thenReturn(List.of(assignment(t, kid)));
        when(emailSender.send(eq("kuttima@example.com"), anyString(), anyString()))
                .thenReturn(SendResult.success());

        List<NotificationService.ReminderResult> results = service.sendReminderForTask(2L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).channel()).isEqualTo("EMAIL");
        assertThat(results.get(0).sent()).isTrue();

        verify(telegramSender, never()).send(any(), any(), any());
        verify(logRepository, times(1)).save(any());
    }

    /**
     * Kid with no contact info: skipped entirely, no send, no log row.
     */
    @Test
    void kidWithNoContact_skipped_noSendNoLogRow() {
        Task t = task(3L, "Clean room");
        User kid = kid(12L, "Vicky", null, null);
        when(taskRepository.findById(3L)).thenReturn(Optional.of(t));
        when(assignmentRepository.findByTaskId(3L)).thenReturn(List.of(assignment(t, kid)));

        List<NotificationService.ReminderResult> results = service.sendReminderForTask(3L);

        // Skipped result returned with sent=false and skip reason
        assertThat(results).hasSize(1);
        assertThat(results.get(0).sent()).isFalse();
        assertThat(results.get(0).skipped()).isTrue();

        verify(emailSender, never()).send(any(), any(), any());
        verify(telegramSender, never()).send(any(), any(), any());
        verify(logRepository, never()).save(any());
    }

    /**
     * Channel send fails: log row written with FAILED status and error text.
     */
    @Test
    void channelSendFails_logRowWrittenWithFailedStatus() {
        Task t = task(4L, "Exercise");
        User kid = kid(13L, "Maya", "maya@example.com", null);
        when(taskRepository.findById(4L)).thenReturn(Optional.of(t));
        when(assignmentRepository.findByTaskId(4L)).thenReturn(List.of(assignment(t, kid)));
        when(emailSender.send(eq("maya@example.com"), anyString(), anyString()))
                .thenReturn(SendResult.fail("SMTP timeout"));

        List<NotificationService.ReminderResult> results = service.sendReminderForTask(4L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).sent()).isFalse();
        assertThat(results.get(0).skipped()).isFalse();
        assertThat(results.get(0).error()).contains("SMTP timeout");

        var captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(captor.getValue().getError()).contains("SMTP timeout");
    }

    /**
     * Task not found: 404 ResponseStatusException thrown.
     */
    @Test
    void taskNotFound_throwsNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendReminderForTask(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    /**
     * Multiple kids assigned — each processed independently, per-kid per-channel result.
     */
    @Test
    void multipleKidsAssigned_eachKidProcessedIndependently() {
        Task t = task(5L, "Dinner");
        User kid1 = kid(20L, "Ammu", "ammu@example.com", null);
        User kid2 = kid(21L, "Kuttima", null, -5139466273L);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(t));
        when(assignmentRepository.findByTaskId(5L)).thenReturn(
                List.of(assignment(t, kid1), assignment(t, kid2)));
        when(emailSender.send(eq("ammu@example.com"), anyString(), anyString()))
                .thenReturn(SendResult.success());
        when(telegramSender.send(eq("-5139466273"), anyString(), anyString()))
                .thenReturn(SendResult.success());

        List<NotificationService.ReminderResult> results = service.sendReminderForTask(5L);

        // 2 results: one per kid-channel pair
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(NotificationService.ReminderResult::sent);
        verify(logRepository, times(2)).save(any());
    }
}
