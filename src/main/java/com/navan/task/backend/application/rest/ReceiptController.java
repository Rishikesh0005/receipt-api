package com.navan.task.backend.application.rest;

import com.navan.task.backend.application.dto.ProcessResponseDto;
import com.navan.task.backend.application.dto.ReceiptUploadResponseDto;
import com.navan.task.backend.domain.model.Receipt;
import com.navan.task.backend.domain.model.Transaction;
import com.navan.task.backend.domain.service.ReceiptIngestionService;
import com.navan.task.backend.domain.service.ReceiptUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST endpoint for receipt upload and processing. All business logic
 * lives in the service layer; this class only wires HTTP requests to
 * services and shapes their results into response DTOs.
 */
@RestController
@RequestMapping("/receipts")
public class ReceiptController {

    private final ReceiptUploadService receiptUploadService;
    private final ReceiptIngestionService receiptIngestionService;

    public ReceiptController(ReceiptUploadService receiptUploadService,
                              ReceiptIngestionService receiptIngestionService) {
        this.receiptUploadService = receiptUploadService;
        this.receiptIngestionService = receiptIngestionService;
    }

    /**
     * POST /receipts - Upload a receipt file.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReceiptUploadResponseDto upload(@RequestParam("file") MultipartFile file) {
        Receipt receipt = receiptUploadService.upload(file);
        return new ReceiptUploadResponseDto(receipt.getId(), "Receipt uploaded successfully");
    }

    /**
     * POST /receipts/{id}/process - Run OCR and create the transaction.
     */
    @PostMapping("/{id}/process")
    public ProcessResponseDto process(@PathVariable String id) {
        Transaction transaction = receiptIngestionService.process(id);
        return new ProcessResponseDto(transaction.getId(), transaction.getItemizeStatus().toString());
    }
}

