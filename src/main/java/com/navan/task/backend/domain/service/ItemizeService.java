package com.navan.task.backend.domain.service;

import com.navan.task.backend.domain.exception.OcrExtractionException;
import com.navan.task.backend.domain.exception.ReceiptNotFoundException;
import com.navan.task.backend.domain.exception.TransactionNotFoundException;
import com.navan.task.backend.domain.model.Receipt;
import com.navan.task.backend.domain.model.Transaction;
import com.navan.task.backend.domain.port.ReceiptRepository;
import com.navan.task.backend.domain.port.TransactionRepository;

/**
 * Orchestrates POST /transactions/{id}/itemize: re-run auto-itemize from
 * the STORED raw OCR text (never re-reads the file or calls OCR again),
 * replacing line items only. The transaction header and taxes are left
 * untouched, and no second transaction is ever created.
 */
public class ItemizeService {

    private final TransactionRepository transactionRepository;
    private final ReceiptRepository receiptRepository;
    private final ReceiptProcessingService receiptProcessingService;

    public ItemizeService(TransactionRepository transactionRepository,
                           ReceiptRepository receiptRepository,
                           ReceiptProcessingService receiptProcessingService) {
        this.transactionRepository = transactionRepository;
        this.receiptRepository = receiptRepository;
        this.receiptProcessingService = receiptProcessingService;
    }

    public Transaction reItemize(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        Receipt receipt = receiptRepository.findById(transaction.getReceiptId())
                .orElseThrow(() -> new ReceiptNotFoundException(transaction.getReceiptId()));

        String ocrText = receipt.getRawOcrText();
        if (ocrText == null || ocrText.isBlank()) {
            throw new OcrExtractionException("No stored OCR text for transaction " + transactionId);
        }

        receiptProcessingService.reItemize(transaction, ocrText);
        return transaction;
    }
}
