package com.navan.task.backend.application.dto;

/**
 * DTO for process response.
 */
public class ProcessResponseDto {
    public String transactionId;
    public String status;

    public ProcessResponseDto() {
    }

    public ProcessResponseDto(String transactionId, String status) {
        this.transactionId = transactionId;
        this.status = status;
    }
}
