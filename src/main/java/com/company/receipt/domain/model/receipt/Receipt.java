package com.company.receipt.domain.model.receipt;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Receipt aggregate root.
 * Represents an uploaded receipt file with OCR results.
 */
public class Receipt {
    private final ReceiptId id;
    private final String filePath;
    private final String fileName;
    private final LocalDateTime uploadedAt;
    private String rawOcrText;

    private Receipt(ReceiptId id, String filePath, String fileName, LocalDateTime uploadedAt) {
        this.id = Objects.requireNonNull(id);
        this.filePath = Objects.requireNonNull(filePath);
        this.fileName = Objects.requireNonNull(fileName);
        this.uploadedAt = Objects.requireNonNull(uploadedAt);
    }

    public static Receipt create(String filePath, String fileName) {
        return new Receipt(
                ReceiptId.generate(),
                filePath,
                fileName,
                LocalDateTime.now()
        );
    }

    public static Receipt restore(ReceiptId id, String filePath, String fileName, LocalDateTime uploadedAt) {
        return new Receipt(id, filePath, fileName, uploadedAt);
    }

    public ReceiptId getId() {
        return id;
    }

    public String getFilePath() {
        return filePath;
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
