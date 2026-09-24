package com.navan.task.backend.application.dto;

import java.util.List;

/**
 * DTO for itemize update request.
 */
public class ItemizeUpdateDto {
    public List<LineItemUpdateDto> items;

    public ItemizeUpdateDto() {
    }

    public ItemizeUpdateDto(List<LineItemUpdateDto> items) {
        this.items = items;
    }

    public static class LineItemUpdateDto {
        public String id;
        public String description;
        public java.math.BigDecimal amount;
        public java.math.BigDecimal quantity;
        public java.math.BigDecimal taxAmount;

        public LineItemUpdateDto() {
        }
    }
}
