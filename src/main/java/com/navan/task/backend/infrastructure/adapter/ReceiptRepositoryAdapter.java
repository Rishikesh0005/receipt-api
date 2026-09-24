package com.navan.task.backend.infrastructure.adapter;

import com.navan.task.backend.domain.model.Receipt;
import com.navan.task.backend.domain.port.ReceiptRepository;
import com.navan.task.backend.infrastructure.persistence.JpaReceiptRepository;
import com.navan.task.backend.infrastructure.persistence.ReceiptEntity;
import org.springframework.stereotype.Component;
import java.util.Optional;

/**
 * Adapter: implements domain ReceiptRepository using Spring Data JPA.
 */
@Component
public class ReceiptRepositoryAdapter implements ReceiptRepository {
    private final JpaReceiptRepository jpaRepository;

    public ReceiptRepositoryAdapter(JpaReceiptRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Receipt save(Receipt receipt) {
        ReceiptEntity entity = new ReceiptEntity(
                receipt.getId(),
                receipt.getFilePath(),
                receipt.getFileName(),
                receipt.getUploadedAt()
        );
        entity.setRawOcrText(receipt.getRawOcrText());
        jpaRepository.save(entity);
        return receipt;
    }

    @Override
    public Optional<Receipt> findById(String id) {
        return jpaRepository.findById(id).map(entity -> {
            Receipt receipt = Receipt.restore(
                    entity.getId(),
                    entity.getFilePath(),
                    entity.getFileName(),
                    entity.getUploadedAt()
            );
            receipt.setRawOcrText(entity.getRawOcrText());
            return receipt;
        });
    }
}
