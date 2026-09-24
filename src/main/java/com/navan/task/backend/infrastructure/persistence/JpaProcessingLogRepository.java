package com.navan.task.backend.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Spring Data JPA repository for audit/activity log entries.
 */
public interface JpaProcessingLogRepository extends JpaRepository<ProcessingLogEntity, String> {
    List<ProcessingLogEntity> findByEntityIdOrderByTimestampDesc(String entityId);
    List<ProcessingLogEntity> findAllByOrderByTimestampDesc(Pageable pageable);
}
