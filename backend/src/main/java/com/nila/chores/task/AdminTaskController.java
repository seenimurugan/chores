package com.nila.chores.task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tasks")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTaskController {
    private final TaskService service;

    public AdminTaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    public List<TaskDto> list() {
        return service.listAll().stream().map(TaskDto::of).toList();
    }

    @PostMapping
    public ResponseEntity<TaskDto> create(@Valid @RequestBody TaskRequest req) {
        Task.Recurrence rec = req.recurrence() == null ? null : Task.Recurrence.valueOf(req.recurrence());
        Task t = service.create(req.title().trim(), req.description(),
                req.points() == null ? 1 : req.points(), req.icon(), rec);
        return ResponseEntity.ok(TaskDto.of(t));
    }

    @PutMapping("/{id}")
    public TaskDto update(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest req) {
        Task.Recurrence rec = req.recurrence() == null ? null : Task.Recurrence.valueOf(req.recurrence());
        Task t = service.update(id, req.title(), req.description(), req.points(), req.icon(), rec, req.active());
        return TaskDto.of(t);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
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
            String recurrence
    ) {}

    public record TaskUpdateRequest(
            @Size(max = 200) String title,
            @Size(max = 4000) String description,
            @Min(0) Integer points,
            @Size(max = 8) String icon,
            String recurrence,
            Boolean active
    ) {}

    public record TaskDto(Long id, String title, String description, Integer points,
                          String icon, String recurrence, Boolean active) {
        public static TaskDto of(Task t) {
            return new TaskDto(t.getId(), t.getTitle(), t.getDescription(), t.getPoints(),
                    t.getIcon(), t.getRecurrence().name(), t.getActive());
        }
    }
}
