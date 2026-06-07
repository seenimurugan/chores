package com.nila.chores.task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.nila.chores.security.AuthUser;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tasks")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTaskController {

    private static final Logger log = LoggerFactory.getLogger(AdminTaskController.class);

    private final TaskService service;

    public AdminTaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    public List<TaskDto> list(@AuthenticationPrincipal AuthUser actor) {
        log.info("event=admin.tasks.list actor={}", actor.id());
        return service.listAll().stream().map(TaskDto::of).toList();
    }

    @PostMapping
    public ResponseEntity<TaskDto> create(@AuthenticationPrincipal AuthUser actor,
                                          @Valid @RequestBody TaskRequest req) {
        log.info("event=admin.tasks.create actor={} title={} weeklyTarget={} remindLeadDays={}",
                actor.id(), req.title(), req.weeklyTarget(), req.remindLeadDays());
        Task.Recurrence rec = req.recurrence() == null ? null : Task.Recurrence.valueOf(req.recurrence());
        Task t = service.create(req.title().trim(), req.description(),
                req.points() == null ? 1 : req.points(), req.icon(), rec,
                req.weeklyTarget(), req.remindLeadDays() == null ? 0 : req.remindLeadDays());
        log.info("event=admin.tasks.create actor={} taskId={} outcome=success", actor.id(), t.getId());
        return ResponseEntity.ok(TaskDto.of(t));
    }

    @PutMapping("/{id}")
    public TaskDto update(@AuthenticationPrincipal AuthUser actor,
                          @PathVariable Long id, @Valid @RequestBody TaskUpdateRequest req) {
        log.info("event=admin.tasks.update actor={} taskId={} weeklyTarget={} remindLeadDays={}",
                actor.id(), id, req.weeklyTarget(), req.remindLeadDays());
        Task.Recurrence rec = req.recurrence() == null ? null : Task.Recurrence.valueOf(req.recurrence());
        Task t = service.update(id, req.title(), req.description(), req.points(), req.icon(), rec,
                req.active(), req.weeklyTarget(), req.remindLeadDays());
        log.info("event=admin.tasks.update actor={} taskId={} outcome=success", actor.id(), id);
        return TaskDto.of(t);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        log.info("event=admin.tasks.delete actor={} taskId={}", actor.id(), id);
        service.delete(id);
        log.info("event=admin.tasks.delete actor={} taskId={} outcome=success", actor.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/assignees")
    public List<Long> assignees(@PathVariable Long id) {
        return service.getAssignments(id).stream().map(a -> a.getUser().getId()).toList();
    }

    @PostMapping("/{id}/assign/{userId}")
    public ResponseEntity<Void> assign(@PathVariable Long id, @PathVariable Long userId) {
        service.assign(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/assign/{userId}")
    public ResponseEntity<Void> unassign(@PathVariable Long id, @PathVariable Long userId) {
        service.unassign(id, userId);
        return ResponseEntity.noContent().build();
    }

    public record TaskRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 4000) String description,
            @Min(0) Integer points,
            @Size(max = 8) String icon,
            String recurrence,
            /** How many times per week. Must be > 0 when set. Null = not tracked. */
            @Min(1) Integer weeklyTarget,
            /** How many days early to send at-risk reminder. Must be >= 0. */
            @Min(0) Integer remindLeadDays
    ) {}

    public record TaskUpdateRequest(
            @Size(max = 200) String title,
            @Size(max = 4000) String description,
            @Min(0) Integer points,
            @Size(max = 8) String icon,
            String recurrence,
            Boolean active,
            /** How many times per week. Must be > 0 when set. Null = not tracked. */
            @Min(1) Integer weeklyTarget,
            /** How many days early to send at-risk reminder. Must be >= 0. */
            @Min(0) Integer remindLeadDays
    ) {}

    public record TaskDto(Long id, String title, String description, Integer points,
                          String icon, String recurrence, Boolean active,
                          Integer weeklyTarget, Integer remindLeadDays) {
        public static TaskDto of(Task t) {
            return new TaskDto(t.getId(), t.getTitle(), t.getDescription(), t.getPoints(),
                    t.getIcon(), t.getRecurrence().name(), t.getActive(),
                    t.getWeeklyTarget(), t.getRemindLeadDays());
        }
    }
}
