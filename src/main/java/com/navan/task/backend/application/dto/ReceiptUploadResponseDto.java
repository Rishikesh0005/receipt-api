package com.navan.task.backend.application.dto;

/**
 * DTO for receipt upload response.
 */
public class ReceiptUploadResponseDto {
    public String receiptId;
    public String message;

    public ReceiptUploadResponseDto() {
    }

    public ReceiptUploadResponseDto(String receiptId, String message) {
        this.receiptId = receiptId;
        this.message = message;
    }
}
