package com.nila.chores.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskCompletionRepository extends JpaRepository<TaskCompletion, Long> {

    Optional<TaskCompletion> findByTaskIdAndUserIdAndCompletionDate(Long taskId, Long userId, LocalDate date);

    @Query("""
        select c from TaskCompletion c
        where c.user.id = :userId and c.completionDate = :date
    """)
    List<TaskCompletion> findForUserOn(Long userId, LocalDate date);

    @Query("""
        select c.completionDate as day, count(c) as done
        from TaskCompletion c
        where c.user.id = :userId and c.done = true
          and c.completionDate between :from and :to
        group by c.completionDate
        order by c.completionDate asc
    """)
    List<DailyCount> countDoneByDay(Long userId, LocalDate from, LocalDate to);

    @Query("""
        select c.completionDate as day, c.user.id as userId, c.task.id as taskId
        from TaskCompletion c
        where c.user.id = :userId and c.done = true
          and c.completionDate between :from and :to
    """)
    List<CompletionRow> listForUser(Long userId, LocalDate from, LocalDate to);

    @Query("""
        select c.completionDate as day, c.user.id as userId, c.task.id as taskId
        from TaskCompletion c
        where c.done = true and c.user.role = com.nila.chores.user.User.Role.KID
          and c.completionDate between :from and :to
    """)
    List<CompletionRow> listForAllKids(LocalDate from, LocalDate to);

    /**
     * Count done=true completions for a specific (userId, taskId) pair within a date range.
     * Used by the at-risk scheduler and reminder overview to compute per-chore progress.
     */
    @Query("""
        select count(c) from TaskCompletion c
        where c.user.id = :userId and c.task.id = :taskId and c.done = true
          and c.completionDate between :from and :to
    """)
    long countDoneForUserTask(Long userId, Long taskId, LocalDate from, LocalDate to);

    interface DailyCount {
        LocalDate getDay();
        Long getDone();
    }

    interface CompletionRow {
        LocalDate getDay();
        Long getUserId();
        Long getTaskId();
    }
}
