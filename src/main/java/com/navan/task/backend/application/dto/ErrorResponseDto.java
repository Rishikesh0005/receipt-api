package com.navan.task.backend.application.dto;

/**
 * Standard error response shape returned by the global exception handler.
 */
public class ErrorResponseDto {
    public String code;
    public String message;

    public ErrorResponseDto() {
    }

    public ErrorResponseDto(String code, String message) {
        this.code = code;
        this.message = message;
    }
}

