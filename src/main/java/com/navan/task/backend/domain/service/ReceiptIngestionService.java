package com.navan.task.backend.domain.service;

import com.navan.task.backend.domain.exception.OcrExtractionException;
import com.navan.task.backend.domain.exception.ReceiptNotFoundException;
import com.navan.task.backend.domain.model.Receipt;
import com.navan.task.backend.domain.model.Transaction;
import com.navan.task.backend.domain.port.OcrService;
import com.navan.task.backend.domain.port.ReceiptRepository;

/**
 * Orchestrates POST /receipts/{id}/process: looks up the stored receipt,
 * runs OCR once, persists the raw OCR text for later re-itemize calls,
 * and delegates structured extraction to {@link ReceiptProcessingService}.
 * Exactly one transaction is created per receipt.
 */
public class ReceiptIngestionService {

    private final ReceiptRepository receiptRepository;
    private final OcrService ocrService;
    private final ReceiptProcessingService receiptProcessingService;

    public ReceiptIngestionService(ReceiptRepository receiptRepository,
                                    OcrService ocrService,
                                    ReceiptProcessingService receiptProcessingService) {
        this.receiptRepository = receiptRepository;
        this.ocrService = ocrService;
        this.receiptProcessingService = receiptProcessingService;
    }

    public Transaction process(String receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ReceiptNotFoundException(receiptId));

        String ocrText = ocrService.extractText(receipt.getFilePath());
        if (ocrText == null || ocrText.isBlank()) {
            throw new OcrExtractionException("Failed to extract text from receipt " + receiptId);
        }

        receipt.setRawOcrText(ocrText);
        receiptRepository.save(receipt);

        return receiptProcessingService.processReceipt(receiptId, receipt.getFilePath());
    }
}
