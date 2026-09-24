package com.navan.task.backend.infrastructure.adapter;

import com.navan.task.backend.domain.model.LineItem;
import com.navan.task.backend.domain.model.TaxLine;
import com.navan.task.backend.domain.model.Transaction;
import com.navan.task.backend.domain.port.TransactionRepository;
import com.navan.task.backend.infrastructure.persistence.*;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter: implements domain TransactionRepository using Spring Data JPA.
 */
@Component
public class TransactionRepositoryAdapter implements TransactionRepository {
    private final JpaTransactionRepository jpaRepository;

    public TransactionRepositoryAdapter(JpaTransactionRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity entity = new TransactionEntity();
        entity.setId(transaction.getId());
        entity.setReceiptId(transaction.getReceiptId());
        entity.setMerchant(transaction.getMerchant());
        entity.setDate(transaction.getDate());
        entity.setCurrency(transaction.getCurrency());
        entity.setTotal(transaction.getTotal());
        entity.setItemizeStatus(mapItemizeStatus(transaction.getItemizeStatus()));

        // Map taxes
        List<TaxLineEntity> taxEntities = transaction.getTaxes().stream()
                .map(tax -> {
                    TaxLineEntity e = new TaxLineEntity();
                    e.setId(tax.getId());
                    e.setName(tax.getName());
                    e.setRate(tax.getRate());
                    e.setAmount(tax.getAmount());
                    e.setJurisdiction(tax.getJurisdiction());
                    e.setTransactionId(transaction.getId());
                    return e;
                })
                .collect(Collectors.toList());
        entity.setTaxes(taxEntities);

        // Map items
        List<LineItemEntity> itemEntities = transaction.getItems().stream()
                .map(item -> {
                    LineItemEntity e = new LineItemEntity();
                    e.setId(item.getId());
                    e.setDescription(item.getDescription());
                    e.setAmount(item.getAmount());
                    e.setQuantity(item.getQuantity());
                    e.setTaxAmount(item.getTaxAmount());
                    e.setTransactionId(transaction.getId());
                    return e;
                })
                .collect(Collectors.toList());
        entity.setItems(itemEntities);

        jpaRepository.save(entity);
        return transaction;
    }

    @Override
    public Optional<Transaction> findById(String id) {
        return jpaRepository.findById(id).map(this::mapToTransaction);
    }

    @Override
    public Optional<Transaction> findByReceiptId(String receiptId) {
        return jpaRepository.findByReceiptId(receiptId).map(this::mapToTransaction);
    }

    private Transaction mapToTransaction(TransactionEntity entity) {
        Transaction transaction = Transaction.restore(entity.getId(), entity.getReceiptId());
        transaction.setMerchant(entity.getMerchant());
        transaction.setDate(entity.getDate());
        transaction.setCurrency(entity.getCurrency());
        transaction.setTotal(entity.getTotal());
        transaction.setItemizeStatus(mapItemizeStatus(entity.getItemizeStatus()));

        // Map taxes
        List<TaxLine> taxes = entity.getTaxes().stream()
                .map(e -> {
                    TaxLine tax = TaxLine.restore(e.getId(), e.getName(), e.getRate(), e.getAmount());
                    tax.setJurisdiction(e.getJurisdiction());
                    return tax;
                })
                .collect(Collectors.toList());
        transaction.setTaxes(taxes);

        // Map items
        List<LineItem> items = entity.getItems().stream()
                .map(e -> {
                    LineItem item = LineItem.restore(e.getId(), e.getDescription(), e.getAmount());
                    item.setQuantity(e.getQuantity());
                    item.setTaxAmount(e.getTaxAmount());
                    return item;
                })
                .collect(Collectors.toList());
        transaction.setItems(items);

        return transaction;
    }

    private TransactionEntity.ItemizeStatus mapItemizeStatus(Transaction.ItemizeStatus status) {
        return TransactionEntity.ItemizeStatus.valueOf(status.name());
    }

    private Transaction.ItemizeStatus mapItemizeStatus(TransactionEntity.ItemizeStatus status) {
        return Transaction.ItemizeStatus.valueOf(status.name());
    }
}
