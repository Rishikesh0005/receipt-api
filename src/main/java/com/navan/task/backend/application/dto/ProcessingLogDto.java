package com.navan.task.backend.application.dto;

import com.navan.task.backend.domain.model.ProcessingLog;

import java.time.Instant;

/**
 * Response shape for a single audit/activity log entry.
 */
public class ProcessingLogDto {
    public String id;
    public String entityType;
    public String entityId;
    public String action;
    public String level;
    public String message;
    public Instant timestamp;

    public ProcessingLogDto() {
    }

    public ProcessingLogDto(String id, String entityType, String entityId, String action,
                             String level, String message, Instant timestamp) {
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.level = level;
        this.message = message;
        this.timestamp = timestamp;
    }

    public static ProcessingLogDto from(ProcessingLog log) {
        return new ProcessingLogDto(
                log.getId(),
                log.getEntityType(),
                log.getEntityId(),
                log.getAction(),
                log.getLevel().name(),
                log.getMessage(),
                log.getTimestamp()
        );
    }
}
