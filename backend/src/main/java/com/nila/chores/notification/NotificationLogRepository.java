package com.nila.chores.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    /**
     * Dedup check: has a SENT row already been written for this (kid, task, channel) today?
     * Used by the at-risk scheduler to avoid re-sending on repeat ticks.
     */
    boolean existsByUserIdAndTaskIdAndChannelAndSentAtBetween(
            Long userId, Long taskId, String channel,
            OffsetDateTime from, OffsetDateTime to);

    /**
     * Count SENT rows for a (kid, task) pair within the current week window.
     * Used by the reminder overview endpoint.
     */
    @Query("""
        select count(n) from NotificationLog n
        where n.user.id = :userId and n.task.id = :taskId
          and n.sentAt between :from and :to
          and n.status = 'SENT'
    """)
    long countByUserIdAndTaskIdAndSentAtBetween(Long userId, Long taskId, OffsetDateTime from, OffsetDateTime to);

    /**
     * Find the most recent sentAt for a (kid, task) pair within the current week.
     * Used by the reminder overview endpoint.
     */
    @Query("""
        select max(n.sentAt) from NotificationLog n
        where n.user.id = :userId and n.task.id = :taskId
          and n.sentAt between :from and :to
          and n.status = 'SENT'
    """)
    Optional<OffsetDateTime> findMaxSentAtByUserIdAndTaskIdAndSentAtBetween(
            Long userId, Long taskId, OffsetDateTime from, OffsetDateTime to);
}
