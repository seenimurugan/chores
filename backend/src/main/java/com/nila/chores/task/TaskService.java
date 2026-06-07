package com.nila.chores.task;

import com.nila.chores.user.User;
import com.nila.chores.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository tasks;
    private final TaskAssignmentRepository assignments;
    private final TaskCompletionRepository completions;
    private final UserRepository users;

    public TaskService(TaskRepository tasks,
                       TaskAssignmentRepository assignments,
                       TaskCompletionRepository completions,
                       UserRepository users) {
        this.tasks = tasks;
        this.assignments = assignments;
        this.completions = completions;
        this.users = users;
    }

    public List<Task> listAll() {
        return tasks.findAllByOrderByActiveDescTitleAsc();
    }

    @Transactional
    public Task create(String title, String description, int points, String icon,
                       Task.Recurrence recurrence, Integer weeklyTarget, int remindLeadDays) {
        log.info("event=task.create title={} weeklyTarget={} remindLeadDays={}", title, weeklyTarget, remindLeadDays);
        Task t = new Task();
        t.setTitle(title);
        t.setDescription(description);
        t.setPoints(points);
        t.setIcon(icon);
        t.setRecurrence(recurrence != null ? recurrence : Task.Recurrence.DAILY);
        t.setActive(true);
        t.setWeeklyTarget(weeklyTarget);
        t.setRemindLeadDays(remindLeadDays);
        Task saved = tasks.save(t);
        log.info("event=task.create taskId={} outcome=success", saved.getId());
        return saved;
    }

    @Transactional
    public Task update(Long id, String title, String description, Integer points, String icon,
                       Task.Recurrence recurrence, Boolean active, Integer weeklyTarget, Integer remindLeadDays) {
        log.info("event=task.update taskId={} weeklyTarget={} remindLeadDays={}", id, weeklyTarget, remindLeadDays);
        Task t = tasks.findById(id)
                .orElseThrow(() -> {
                    log.warn("event=task.update taskId={} outcome=fail reason=not-found", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
                });
        if (title != null) t.setTitle(title);
        if (description != null) t.setDescription(description);
        if (points != null) t.setPoints(points);
        if (icon != null) t.setIcon(icon);
        if (recurrence != null) t.setRecurrence(recurrence);
        if (active != null) t.setActive(active);
        // weeklyTarget: null in request means "don't change"; to clear it, pass 0 isn't valid so
        // we use a sentinel: if the field is explicitly in the payload it will be non-null here.
        if (weeklyTarget != null) t.setWeeklyTarget(weeklyTarget);
        if (remindLeadDays != null) t.setRemindLeadDays(remindLeadDays);
        log.info("event=task.update taskId={} outcome=success", id);
        return t;
    }

    @Transactional
    public void delete(Long id) {
        if (!tasks.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
        tasks.deleteById(id);
    }

    public List<TaskAssignment> getAssignments(Long taskId) {
        return assignments.findByTaskId(taskId);
    }

    @Transactional
    public void assign(Long taskId, Long userId) {
        if (assignments.existsByTaskIdAndUserId(taskId, userId)) return;
        Task task = tasks.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        User user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() != User.Role.KID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tasks can only be assigned to kids");
        }
        TaskAssignment a = new TaskAssignment();
        a.setTask(task);
        a.setUser(user);
        assignments.save(a);
    }

    @Transactional
    public void unassign(Long taskId, Long userId) {
        assignments.deleteByTaskIdAndUserId(taskId, userId);
    }

    public List<TaskAssignment> myActiveTasks(Long userId) {
        return assignments.findActiveForUser(userId);
    }

    public List<TaskCompletion> myCompletionsOn(Long userId, LocalDate date) {
        return completions.findForUserOn(userId, date);
    }

    @Transactional
    public TaskCompletion setCompletion(Long taskId, Long userId, LocalDate date, boolean done) {
        if (!assignments.existsByTaskIdAndUserId(taskId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Task is not assigned to you");
        }
        User kid = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        long daysAgo = java.time.temporal.ChronoUnit.DAYS.between(date, LocalDate.now());
        if (daysAgo < 0 || daysAgo > kid.getEditWindowDays()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Date " + date + " is outside your edit window (" + kid.getEditWindowDays() + " days)");
        }
        TaskCompletion c = completions.findByTaskIdAndUserIdAndCompletionDate(taskId, userId, date)
                .orElseGet(() -> {
                    TaskCompletion nc = new TaskCompletion();
                    Task t = tasks.getReferenceById(taskId);
                    User u = users.getReferenceById(userId);
                    nc.setTask(t);
                    nc.setUser(u);
                    nc.setCompletionDate(date);
                    return nc;
                });
        c.setDone(done);
        return completions.save(c);
    }
}
