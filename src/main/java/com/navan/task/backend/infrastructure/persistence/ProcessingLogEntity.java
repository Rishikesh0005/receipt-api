package com.navan.task.backend.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity for storing audit/activity log entries.
 */
@Entity
@Table(name = "processing_logs")
public class ProcessingLogEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private String entityId;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Level level;

    @Column(length = 2000)
    private String message;

    @Column(nullable = false)
    private Instant timestamp;

    public enum Level {
        INFO, WARN, ERROR
    }

    public ProcessingLogEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
