package org.example.inventoryapi.service;

import org.example.inventoryapi.dto.WarehouseStockItem;
import org.example.inventoryapi.model.entity.InventoryTransaction;
import org.example.inventoryapi.model.entity.Product;
import org.example.inventoryapi.model.entity.User;
import org.example.inventoryapi.model.enums.Role;
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

    public List<WarehouseStockItem> getWarehouseStock(int warehouseId) {
        return transactionRepository.findStockByWarehouse(warehouseId);
    }

    @Transactional
    public InventoryTransaction stockIn(InventoryTransaction tx) {
        if (tx.getQuantity() <= 0) throw new IllegalArgumentException("Miktar sıfırdan büyük olmalıdır.");
        Product product = productRepository.findById(tx.getProduct().getId())
                .orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı."));
        product.setQuantityInStock(product.getQuantityInStock() + tx.getQuantity());
        productRepository.save(product);
        tx.setProduct(product);
        tx.setType(TransactionType.IN);
        tx.setTransactionDate(LocalDateTime.now());
        return transactionRepository.save(tx);
    }

    @Transactional
    public InventoryTransaction stockOut(InventoryTransaction tx, User requestingUser) {
        if (tx.getQuantity() <= 0) throw new IllegalArgumentException("Miktar sıfırdan büyük olmalıdır.");
        if (requestingUser.getRole() != Role.ADMIN) {
            if (requestingUser.getWarehouse() == null
                    || requestingUser.getWarehouse().getId() != tx.getWarehouse().getId())
                throw new IllegalStateException("Yalnızca kendi deponuzdan stok çıkışı yapabilirsiniz.");
        }
        Product product = productRepository.findById(tx.getProduct().getId())
                .orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı."));
        int warehouseId = tx.getWarehouse().getId();
        long warehouseQty = transactionRepository.getProductStockInWarehouse(product.getId(), warehouseId);
        if (warehouseQty < tx.getQuantity())
            throw new IllegalStateException("Depoda yetersiz stok. Mevcut: " + warehouseQty);
        product.setQuantityInStock(product.getQuantityInStock() - tx.getQuantity());
        productRepository.save(product);
        tx.setProduct(product);
        tx.setType(TransactionType.OUT);
        tx.setTransactionDate(LocalDateTime.now());
        return transactionRepository.save(tx);
    }

    @Transactional
    public InventoryTransaction transfer(InventoryTransaction tx, User requestingUser) {
        if (tx.getQuantity() <= 0) throw new IllegalArgumentException("Miktar sıfırdan büyük olmalıdır.");
        if (tx.getDestinationWarehouse() == null) throw new IllegalArgumentException("Hedef depo seçilmelidir.");
        if (requestingUser.getRole() != Role.ADMIN) {
            if (requestingUser.getWarehouse() == null
                    || requestingUser.getWarehouse().getId() != tx.getWarehouse().getId())
                throw new IllegalStateException("Yalnızca kendi deponuzdan transfer yapabilirsiniz.");
        }
        Product product = productRepository.findById(tx.getProduct().getId())
                .orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı."));
        int warehouseId = tx.getWarehouse().getId();
        long warehouseQty = transactionRepository.getProductStockInWarehouse(product.getId(), warehouseId);
        if (warehouseQty < tx.getQuantity())
            throw new IllegalStateException("Depoda yetersiz stok. Mevcut: " + warehouseQty);
        tx.setProduct(product);
        tx.setType(TransactionType.TRANSFER);
        tx.setTransactionDate(LocalDateTime.now());
        return transactionRepository.save(tx);
    }
}
