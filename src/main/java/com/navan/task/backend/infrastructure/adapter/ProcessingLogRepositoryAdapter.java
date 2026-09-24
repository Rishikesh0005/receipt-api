package com.navan.task.backend.infrastructure.adapter;

import com.navan.task.backend.domain.model.ProcessingLog;
import com.navan.task.backend.domain.port.ProcessingLogRepository;
import com.navan.task.backend.infrastructure.persistence.JpaProcessingLogRepository;
import com.navan.task.backend.infrastructure.persistence.ProcessingLogEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Adapter: implements domain ProcessingLogRepository using Spring Data JPA.
 */
@Component
public class ProcessingLogRepositoryAdapter implements ProcessingLogRepository {

    private final JpaProcessingLogRepository jpaRepository;

    public ProcessingLogRepositoryAdapter(JpaProcessingLogRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ProcessingLog save(ProcessingLog log) {
        ProcessingLogEntity entity = new ProcessingLogEntity();
        entity.setId(log.getId());
        entity.setEntityType(log.getEntityType());
        entity.setEntityId(log.getEntityId());
        entity.setAction(log.getAction());
        entity.setLevel(ProcessingLogEntity.Level.valueOf(log.getLevel().name()));
        entity.setMessage(log.getMessage());
        entity.setTimestamp(log.getTimestamp());
        jpaRepository.save(entity);
        return log;
    }

    @Override
    public List<ProcessingLog> findRecent(int limit) {
        return jpaRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, limit)).stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProcessingLog> findByEntityId(String entityId) {
        return jpaRepository.findByEntityIdOrderByTimestampDesc(entityId).stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    private ProcessingLog mapToDomain(ProcessingLogEntity entity) {
        return ProcessingLog.restore(
                entity.getId(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getAction(),
                ProcessingLog.Level.valueOf(entity.getLevel().name()),
                entity.getMessage(),
                entity.getTimestamp()
        );
    }
}
