package com.navan.task.backend.domain.port;

import com.navan.task.backend.domain.model.ProcessingLog;
import java.util.List;

/**
 * Output port: audit/activity log persistence contract.
 */
public interface ProcessingLogRepository {
    ProcessingLog save(ProcessingLog log);

    /** Most recent first. */
    List<ProcessingLog> findRecent(int limit);

    /** Most recent first, for one receipt/transaction id. */
    List<ProcessingLog> findByEntityId(String entityId);
}
