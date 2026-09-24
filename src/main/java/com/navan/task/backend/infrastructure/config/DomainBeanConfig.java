package com.navan.task.backend.infrastructure.config;

import com.navan.task.backend.domain.port.FileStorage;
import com.navan.task.backend.domain.port.OcrService;
import com.navan.task.backend.domain.port.ReceiptRepository;
import com.navan.task.backend.domain.port.TransactionRepository;
import com.navan.task.backend.domain.service.ItemPatchService;
import com.navan.task.backend.domain.service.ItemizeService;
import com.navan.task.backend.domain.service.ReceiptIngestionService;
import com.navan.task.backend.domain.service.ReceiptProcessingService;
import com.navan.task.backend.domain.service.ReceiptUploadService;
import com.navan.task.backend.domain.service.ReconciliationPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-free domain services with their port
 * implementations. Domain services themselves have no Spring
 * dependencies; this is the only place that knows about @Bean/@Configuration.
 */
@Configuration
public class DomainBeanConfig {

    @Bean
    public ReconciliationPolicy reconciliationPolicy() {
        return new ReconciliationPolicy();
    }

    @Bean
    public ReceiptProcessingService receiptProcessingService(
            TransactionRepository transactionRepository,
            OcrService ocrService) {
        return new ReceiptProcessingService(transactionRepository, ocrService);
    }

    @Bean
    public ReceiptUploadService receiptUploadService(
            FileStorage fileStorage,
            ReceiptRepository receiptRepository) {
        return new ReceiptUploadService(fileStorage, receiptRepository);
    }

    @Bean
    public ReceiptIngestionService receiptIngestionService(
            ReceiptRepository receiptRepository,
            OcrService ocrService,
            ReceiptProcessingService receiptProcessingService) {
        return new ReceiptIngestionService(receiptRepository, ocrService, receiptProcessingService);
    }

    @Bean
    public ItemizeService itemizeService(
            TransactionRepository transactionRepository,
            ReceiptRepository receiptRepository,
            ReceiptProcessingService receiptProcessingService) {
        return new ItemizeService(transactionRepository, receiptRepository, receiptProcessingService);
    }

    @Bean
    public ItemPatchService itemPatchService(
            TransactionRepository transactionRepository,
            ReconciliationPolicy reconciliationPolicy) {
        return new ItemPatchService(transactionRepository, reconciliationPolicy);
    }
}

