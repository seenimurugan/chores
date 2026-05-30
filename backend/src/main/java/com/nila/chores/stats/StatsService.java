package com.nila.chores.stats;

import com.nila.chores.task.TaskAssignmentRepository;
import com.nila.chores.task.TaskCompletionRepository;
import com.nila.chores.user.User;
import com.nila.chores.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatsService {
    private final TaskAssignmentRepository assignments;
    private final TaskCompletionRepository completions;
    private final UserRepository users;

    public StatsService(TaskAssignmentRepository assignments,
                        TaskCompletionRepository completions,
                        UserRepository users) {
        this.assignments = assignments;
        this.completions = completions;
        this.users = users;
    }

    public KidStats forUser(Long userId, LocalDate from, LocalDate to) {
        User u = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        int assignedActive = assignments.findActiveForUser(userId).size();
        long totalDays = from.datesUntil(to.plusDays(1)).count();

        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        from.datesUntil(to.plusDays(1)).forEach(d -> counts.put(d, 0L));
        for (TaskCompletionRepository.DailyCount dc : completions.countDoneByDay(userId, from, to)) {
            counts.put(dc.getDay(), dc.getDone());
        }
        List<DayPoint> series = counts.entrySet().stream()
                .map(e -> new DayPoint(e.getKey(), e.getValue().intValue()))
                .toList();

        long totalDone = series.stream().mapToInt(DayPoint::done).sum();
        long expected = (long) assignedActive * totalDays;
        int completionRate = expected == 0 ? 0 : (int) Math.round(100.0 * totalDone / expected);

        return new KidStats(userId, u.getDisplayName(), u.getAvatarColor(), assignedActive,
                (int) totalDone, completionRate, series);
    }

    public List<KidStats> forAllKids(LocalDate from, LocalDate to) {
        List<KidStats> out = new ArrayList<>();
        for (User u : users.findByRoleOrderByDisplayNameAsc(User.Role.KID)) {
            out.add(forUser(u.getId(), from, to));
        }
        return out;
    }

    public KidMatrix matrixForUser(Long userId, LocalDate from, LocalDate to) {
        if (!users.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        List<TaskCol> taskCols = assignments.findAssignedTasksForUser(userId).stream()
                .map(r -> new TaskCol(r.getId(), r.getTitle(), r.getIcon(), r.getPoints() == null ? 0 : r.getPoints()))
                .toList();
        Map<LocalDate, Set<Long>> doneByDay = completions.listForUser(userId, from, to).stream()
                .collect(Collectors.groupingBy(
                        TaskCompletionRepository.CompletionRow::getDay,
                        Collectors.mapping(TaskCompletionRepository.CompletionRow::getTaskId, Collectors.toSet())));
        List<DayRow> rows = from.datesUntil(to.plusDays(1))
                .sorted(Comparator.reverseOrder())
                .map(d -> new DayRow(d, new ArrayList<>(doneByDay.getOrDefault(d, Set.of()))))
                .toList();
        return new KidMatrix(taskCols, rows);
    }

    public AdminMatrix matrixForAllKids(LocalDate from, LocalDate to) {
        List<User> kidUsers = users.findByRoleOrderByDisplayNameAsc(User.Role.KID);

        Map<Long, Set<Long>> assignedByUser = new HashMap<>();
        LinkedHashMap<Long, TaskCol> colsById = new LinkedHashMap<>();
        for (TaskAssignmentRepository.AssignedTaskRow r : assignments.findAssignedTasksForAllKids()) {
            assignedByUser.computeIfAbsent(r.getUserId(), k -> new HashSet<>()).add(r.getId());
            colsById.putIfAbsent(r.getId(),
                    new TaskCol(r.getId(), r.getTitle(), r.getIcon(), r.getPoints() == null ? 0 : r.getPoints()));
        }
        List<TaskCol> taskCols = new ArrayList<>(colsById.values());

        List<KidInfo> kids = kidUsers.stream()
                .map(u -> new KidInfo(u.getId(), u.getDisplayName(), u.getAvatarColor(),
                        new ArrayList<>(assignedByUser.getOrDefault(u.getId(), Set.of()))))
                .toList();

        // (day, userId) -> set of done task ids
        Map<LocalDate, Map<Long, Set<Long>>> doneByDayByUser = new HashMap<>();
        for (TaskCompletionRepository.CompletionRow c : completions.listForAllKids(from, to)) {
            doneByDayByUser
                    .computeIfAbsent(c.getDay(), d -> new HashMap<>())
                    .computeIfAbsent(c.getUserId(), u -> new HashSet<>())
                    .add(c.getTaskId());
        }

        List<AdminDayRow> rows = new ArrayList<>();
        from.datesUntil(to.plusDays(1))
                .sorted(Comparator.reverseOrder())
                .forEach(d -> {
                    Map<Long, Set<Long>> perUser = doneByDayByUser.getOrDefault(d, Map.of());
                    for (KidInfo k : kids) {
                        rows.add(new AdminDayRow(d, k.userId(),
                                new ArrayList<>(perUser.getOrDefault(k.userId(), Set.of()))));
                    }
                });
        return new AdminMatrix(taskCols, kids, rows);
    }

    public record DayPoint(LocalDate day, int done) {}

    public record KidStats(Long userId, String displayName, String avatarColor,
                           int activeTasks, int totalDone, int completionRate,
                           List<DayPoint> series) {}

    public record TaskCol(Long id, String title, String icon, int points) {}

    public record DayRow(LocalDate day, List<Long> doneTaskIds) {}

    public record AdminDayRow(LocalDate day, Long userId, List<Long> doneTaskIds) {}

    public record KidInfo(Long userId, String displayName, String avatarColor, List<Long> assignedTaskIds) {}

    public record KidMatrix(List<TaskCol> tasks, List<DayRow> rows) {}

    public record AdminMatrix(List<TaskCol> tasks, List<KidInfo> kids, List<AdminDayRow> rows) {}
}
