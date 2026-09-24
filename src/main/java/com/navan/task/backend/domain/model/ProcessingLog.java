package com.navan.task.backend.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A single audit/activity log entry recorded whenever something happens
 * to a receipt or transaction (uploaded, processed, itemized, patched,
 * or failed). Stored so the UI can show a full history of what the
 * system did, and so failures are not silently swallowed.
 */
public class ProcessingLog {
    public enum Level {
        INFO, WARN, ERROR
    }

    private final String id;
    private final String entityType; // "RECEIPT" | "TRANSACTION"
    private final String entityId;
    private final String action;     // e.g. "UPLOAD", "PROCESS", "ITEMIZE", "PATCH_ITEMS"
    private final Level level;
    private final String message;
    private final Instant timestamp;

    private ProcessingLog(String id, String entityType, String entityId, String action,
                           Level level, String message, Instant timestamp) {
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.level = level;
        this.message = message;
        this.timestamp = timestamp;
    }

    public static ProcessingLog create(String entityType, String entityId, String action,
                                        Level level, String message) {
        return new ProcessingLog(UUID.randomUUID().toString(), entityType, entityId, action,
                level, message, Instant.now());
    }

    public static ProcessingLog restore(String id, String entityType, String entityId, String action,
                                         Level level, String message, Instant timestamp) {
        return new ProcessingLog(id, entityType, entityId, action, level, message, timestamp);
    }

    public String getId() {
        return id;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getAction() {
        return action;
    }

    public Level getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
