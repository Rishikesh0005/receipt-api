package com.navan.task.backend.domain.service;

import com.navan.task.backend.domain.model.ProcessingLog;
import com.navan.task.backend.domain.port.ProcessingLogRepository;

import java.util.List;

/**
 * Thin, framework-free wrapper around the audit log port. Every other
 * domain service calls this instead of writing to the repository
 * directly, so "what gets logged and how" stays in one place.
 */
public class AuditLogService {

    private final ProcessingLogRepository processingLogRepository;

    public AuditLogService(ProcessingLogRepository processingLogRepository) {
        this.processingLogRepository = processingLogRepository;
    }

    public void info(String entityType, String entityId, String action, String message) {
        log(entityType, entityId, action, ProcessingLog.Level.INFO, message);
    }

    public void warn(String entityType, String entityId, String action, String message) {
        log(entityType, entityId, action, ProcessingLog.Level.WARN, message);
    }

    public void error(String entityType, String entityId, String action, String message) {
        log(entityType, entityId, action, ProcessingLog.Level.ERROR, message);
    }

    private void log(String entityType, String entityId, String action,
                      ProcessingLog.Level level, String message) {
        processingLogRepository.save(ProcessingLog.create(entityType, entityId, action, level, message));
    }

    public List<ProcessingLog> recent(int limit) {
        return processingLogRepository.findRecent(limit);
    }

    public List<ProcessingLog> forEntity(String entityId) {
        return processingLogRepository.findByEntityId(entityId);
    }
}
