package com.nila.chores.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {

    @Query("""
        select a from TaskAssignment a
        join fetch a.task t
        where a.user.id = :userId and t.active = true
        order by t.title asc
    """)
    List<TaskAssignment> findActiveForUser(Long userId);

    @Query("select a from TaskAssignment a join fetch a.user u where a.task.id = :taskId order by u.displayName asc")
    List<TaskAssignment> findByTaskId(Long taskId);

    void deleteByTaskIdAndUserId(Long taskId, Long userId);

    boolean existsByTaskIdAndUserId(Long taskId, Long userId);

    @Query("""
        select t.id as id, t.title as title, t.icon as icon, t.points as points,
               a.user.id as userId
        from TaskAssignment a join a.task t
        where t.active = true and a.user.id = :userId
        order by t.title asc
    """)
    List<AssignedTaskRow> findAssignedTasksForUser(Long userId);

    @Query("""
        select t.id as id, t.title as title, t.icon as icon, t.points as points,
               a.user.id as userId
        from TaskAssignment a join a.task t join a.user u
        where t.active = true and u.role = com.nila.chores.user.User.Role.KID
        order by t.title asc
    """)
    List<AssignedTaskRow> findAssignedTasksForAllKids();

    interface AssignedTaskRow {
        Long getId();
        String getTitle();
        String getIcon();
        Integer getPoints();
        Long getUserId();
    }
}
