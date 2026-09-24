package com.navan.task.backend.application.rest;

import com.navan.task.backend.application.dto.ItemizeUpdateDto;
import com.navan.task.backend.application.dto.TransactionDto;
import com.navan.task.backend.domain.exception.TransactionNotFoundException;
import com.navan.task.backend.domain.model.LineItem;
import com.navan.task.backend.domain.model.Transaction;
import com.navan.task.backend.domain.port.TransactionRepository;
import com.navan.task.backend.domain.service.ItemPatchService;
import com.navan.task.backend.domain.service.ItemizeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST endpoint for transaction retrieval and itemization. All business
 * logic lives in the service layer; this class only wires HTTP requests
 * to services and shapes their results into response DTOs.
 */
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final ItemizeService itemizeService;
    private final ItemPatchService itemPatchService;

    public TransactionController(TransactionRepository transactionRepository,
                                  ItemizeService itemizeService,
                                  ItemPatchService itemPatchService) {
        this.transactionRepository = transactionRepository;
        this.itemizeService = itemizeService;
        this.itemPatchService = itemPatchService;
    }

    /**
     * GET /transactions/{id} - Get transaction with taxes and items.
     */
    @GetMapping("/{id}")
    public TransactionDto getTransaction(@PathVariable String id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        return TransactionDto.from(transaction);
    }

    /**
     * POST /transactions/{id}/itemize - Re-run auto-itemize from stored OCR.
     */
    @PostMapping("/{id}/itemize")
    public TransactionDto reItemize(@PathVariable String id) {
        Transaction transaction = itemizeService.reItemize(id);
        return TransactionDto.from(transaction);
    }

    /**
     * PATCH /transactions/{id}/items - User override of items.
     */
    @PatchMapping("/{id}/items")
    public TransactionDto updateItems(@PathVariable String id, @RequestBody ItemizeUpdateDto dto) {
        List<LineItem> newItems = dto.items.stream()
                .map(this::toDomainItem)
                .collect(Collectors.toList());

        Transaction transaction = itemPatchService.patchItems(id, newItems);
        return TransactionDto.from(transaction);
    }

    private LineItem toDomainItem(ItemizeUpdateDto.LineItemUpdateDto itemDto) {
        LineItem item = new LineItem(itemDto.description, itemDto.amount);
        if (itemDto.quantity != null) {
            item.setQuantity(itemDto.quantity);
        }
        item.setTaxAmount(itemDto.taxAmount);
        return item;
    }
}

