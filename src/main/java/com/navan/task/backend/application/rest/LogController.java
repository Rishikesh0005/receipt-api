package com.navan.task.backend.application.rest;

import com.navan.task.backend.application.dto.ProcessingLogDto;
import com.navan.task.backend.domain.service.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST endpoint for reading the audit/activity log. Every receipt
 * upload, OCR process, itemize and item-patch call writes an entry here
 * via {@link AuditLogService}; this controller only reads it back.
 */
@RestController
@RequestMapping("/logs")
public class LogController {

    private final AuditLogService auditLogService;

    public LogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * GET /logs?limit=100 - Most recent activity across all receipts/transactions.
     */
    @GetMapping
    public List<ProcessingLogDto> recent(@RequestParam(defaultValue = "100") int limit) {
        return auditLogService.recent(limit).stream()
                .map(ProcessingLogDto::from)
                .collect(Collectors.toList());
    }

    /**
     * GET /logs/{entityId} - Activity history for one receipt or transaction id.
     */
    @GetMapping("/{entityId}")
    public List<ProcessingLogDto> forEntity(@PathVariable String entityId) {
        return auditLogService.forEntity(entityId).stream()
                .map(ProcessingLogDto::from)
                .collect(Collectors.toList());
    }
}
