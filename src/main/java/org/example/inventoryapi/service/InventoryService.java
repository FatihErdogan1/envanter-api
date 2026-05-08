package org.example.inventoryapi.service;

import org.example.inventoryapi.model.entity.InventoryTransaction;
import org.example.inventoryapi.model.entity.Product;
import org.example.inventoryapi.model.enums.TransactionType;
import org.example.inventoryapi.repository.InventoryTransactionRepository;
import org.example.inventoryapi.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InventoryService {

    private final InventoryTransactionRepository transactionRepository;
    private final ProductRepository              productRepository;

    public InventoryService(InventoryTransactionRepository transactionRepository,
                            ProductRepository productRepository) {
        this.transactionRepository = transactionRepository;
        this.productRepository     = productRepository;
    }

    public List<InventoryTransaction> getAllTransactions() {
        return transactionRepository.findAllByOrderByTransactionDateDesc();
    }

    @Transactional
    public InventoryTransaction stockIn(InventoryTransaction tx) {
        if (tx.getQuantity() <= 0) throw new IllegalArgumentException("Miktar sıfırdan büyük olmalıdır.");
        tx.setType(TransactionType.IN);
        tx.setTransactionDate(LocalDateTime.now());
        Product product = tx.getProduct();
        product.setQuantityInStock(product.getQuantityInStock() + tx.getQuantity());
        productRepository.save(product);
        return transactionRepository.save(tx);
    }

    @Transactional
    public InventoryTransaction stockOut(InventoryTransaction tx) {
        if (tx.getQuantity() <= 0) throw new IllegalArgumentException("Miktar sıfırdan büyük olmalıdır.");
        Product product = tx.getProduct();
        if (product.getQuantityInStock() < tx.getQuantity())
            throw new IllegalStateException("Yetersiz stok. Mevcut: " + product.getQuantityInStock());
        tx.setType(TransactionType.OUT);
        tx.setTransactionDate(LocalDateTime.now());
        product.setQuantityInStock(product.getQuantityInStock() - tx.getQuantity());
        productRepository.save(product);
        return transactionRepository.save(tx);
    }

    @Transactional
    public InventoryTransaction transfer(InventoryTransaction tx) {
        if (tx.getQuantity() <= 0) throw new IllegalArgumentException("Miktar sıfırdan büyük olmalıdır.");
        if (tx.getDestinationWarehouse() == null) throw new IllegalArgumentException("Hedef depo seçilmelidir.");
        Product product = tx.getProduct();
        if (product.getQuantityInStock() < tx.getQuantity())
            throw new IllegalStateException("Yetersiz stok. Mevcut: " + product.getQuantityInStock());
        tx.setType(TransactionType.TRANSFER);
        tx.setTransactionDate(LocalDateTime.now());
        return transactionRepository.save(tx);
    }
}
