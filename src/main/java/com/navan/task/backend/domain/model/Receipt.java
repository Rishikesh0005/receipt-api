package com.navan.task.backend.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Core domain model for a receipt (no Spring/JPA annotations).
 * Represents a scanned receipt file.
 */
public class Receipt {
    private final String id;
    private String filePath;
    private final String fileName;
    private final LocalDateTime uploadedAt;
    private String rawOcrText;

    public Receipt(String filePath, String fileName) {
        this.id = UUID.randomUUID().toString();
        this.filePath = filePath;
        this.fileName = fileName;
        this.uploadedAt = LocalDateTime.now();
    }

    private Receipt(String id, String filePath, String fileName, LocalDateTime uploadedAt) {
        this.id = id;
        this.filePath = filePath;
        this.fileName = fileName;
        this.uploadedAt = uploadedAt;
    }

    /**
     * Rebuild a Receipt from persisted state, preserving its original id
     * and upload timestamp.
     */
    public static Receipt restore(String id, String filePath, String fileName, LocalDateTime uploadedAt) {
        return new Receipt(id, filePath, fileName, uploadedAt);
    }

    public String getId() {
        return id;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public String getRawOcrText() {
        return rawOcrText;
    }

    public void setRawOcrText(String rawOcrText) {
        this.rawOcrText = rawOcrText;
    }
}
