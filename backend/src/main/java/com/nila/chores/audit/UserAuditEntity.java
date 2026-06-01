package com.nila.chores.audit;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "app_user_audit")
public class UserAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 32)
    private String eventType;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_username", length = 64)
    private String actorUsername;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "source_ip", length = 64)
    private String sourceIp;

    @Column(name = "outcome", nullable = false, length = 16)
    private String outcome;

    @Column(name = "reason", length = 128)
    private String reason;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected UserAuditEntity() {}

    public UserAuditEntity(String eventType, Long actorUserId, String actorUsername,
                           Long targetUserId, String sourceIp, String outcome, String reason) {
        this.eventType = eventType;
        this.actorUserId = actorUserId;
        this.actorUsername = actorUsername;
        this.targetUserId = targetUserId;
        this.sourceIp = sourceIp;
        this.outcome = outcome;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public String getEventType() { return eventType; }
    public Long getActorUserId() { return actorUserId; }
    public String getActorUsername() { return actorUsername; }
    public Long getTargetUserId() { return targetUserId; }
    public String getSourceIp() { return sourceIp; }
    public String getOutcome() { return outcome; }
    public String getReason() { return reason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
