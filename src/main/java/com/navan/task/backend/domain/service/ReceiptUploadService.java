package com.navan.task.backend.domain.service;

import com.navan.task.backend.domain.exception.EmptyFileException;
import com.navan.task.backend.domain.model.Receipt;
import com.navan.task.backend.domain.port.FileStorage;
import com.navan.task.backend.domain.port.ReceiptRepository;
import org.springframework.web.multipart.MultipartFile;

/**
 * Handles receipt upload: validates the incoming file, stores it, and
 * persists the receipt record. This is the only place that knows how
 * "uploading a receipt" works end to end.
 */
public class ReceiptUploadService {

    private final FileStorage fileStorage;
    private final ReceiptRepository receiptRepository;
    private final AuditLogService auditLogService;

    public ReceiptUploadService(FileStorage fileStorage, ReceiptRepository receiptRepository,
                                 AuditLogService auditLogService) {
        this.fileStorage = fileStorage;
        this.receiptRepository = receiptRepository;
        this.auditLogService = auditLogService;
    }

    public Receipt upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new EmptyFileException();
        }

        Receipt receipt = new Receipt(null, file.getOriginalFilename());
        String storedPath = fileStorage.store(file, receipt.getId());
        receipt.setFilePath(storedPath);

        Receipt saved = receiptRepository.save(receipt);
        auditLogService.info("RECEIPT", saved.getId(), "UPLOAD",
                "Uploaded file \"" + file.getOriginalFilename() + "\"");
        return saved;
    }
}
