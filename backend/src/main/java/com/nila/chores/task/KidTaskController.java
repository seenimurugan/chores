package com.nila.chores.task;

import com.nila.chores.security.AuthUser;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/me/tasks")
@PreAuthorize("isAuthenticated()")
public class KidTaskController {
    private final TaskService service;

    public KidTaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    public List<MyTaskDto> myTasks(@AuthenticationPrincipal AuthUser user,
                                   @RequestParam(value = "date", required = false) LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        Map<Long, Boolean> doneByTaskId = new HashMap<>();
        for (TaskCompletion c : service.myCompletionsOn(user.id(), target)) {
            doneByTaskId.put(c.getTask().getId(), c.getDone());
        }
        return service.myActiveTasks(user.id()).stream()
                .map(a -> {
                    Task t = a.getTask();
                    return new MyTaskDto(
                            t.getId(),
                            t.getTitle(),
                            t.getDescription(),
                            t.getPoints(),
                            t.getIcon(),
                            t.getRecurrence().name(),
                            doneByTaskId.getOrDefault(t.getId(), false)
                    );
                })
                .toList();
    }

    @PostMapping("/{id}/check")
    public ResponseEntity<Void> check(@AuthenticationPrincipal AuthUser user,
                                       @PathVariable("id") Long taskId,
                                       @RequestBody @NotNull CheckRequest req) {
        LocalDate target = req.date() != null ? req.date() : LocalDate.now();
        service.setCompletion(taskId, user.id(), target, req.done());
        return ResponseEntity.noContent().build();
    }

    public record CheckRequest(boolean done, LocalDate date) {}

    public record MyTaskDto(Long id, String title, String description, Integer points,
                            String icon, String recurrence, boolean done) {}
}
