package com.navan.task.backend.domain.port;

import com.navan.task.backend.domain.model.Receipt;
import java.util.Optional;

/**
 * Output port: Receipt persistence contract.
 */
public interface ReceiptRepository {
    Receipt save(Receipt receipt);
    Optional<Receipt> findById(String id);
}
