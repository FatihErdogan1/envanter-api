package org.example.inventoryapi.service;

import org.example.inventoryapi.constants.AppConstants;
import org.example.inventoryapi.dto.WarehouseStockItem;
import org.example.inventoryapi.model.entity.InventoryTransaction;
import org.example.inventoryapi.model.entity.Product;
import org.example.inventoryapi.model.entity.User;
import org.example.inventoryapi.model.enums.NotificationType;
import org.example.inventoryapi.model.enums.Role;
import org.example.inventoryapi.model.enums.TransactionType;
import org.example.inventoryapi.repository.InventoryTransactionRepository;
import org.example.inventoryapi.repository.ProductRepository;
import org.example.inventoryapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryTransactionRepository transactionRepository;
    private final ProductRepository              productRepository;
    private final UserRepository                 userRepository;
    private final NotificationService            notificationService;

    public InventoryService(InventoryTransactionRepository transactionRepository,
                            ProductRepository productRepository,
                            UserRepository userRepository,
                            NotificationService notificationService) {
        this.transactionRepository = transactionRepository;
        this.productRepository     = productRepository;
        this.userRepository        = userRepository;
        this.notificationService   = notificationService;
    }

    public List<InventoryTransaction> getAllTransactions() {
        return transactionRepository.findAllByOrderByTransactionDateDesc();
    }

    public List<WarehouseStockItem> getWarehouseStock(int warehouseId) {
        return transactionRepository.findStockByWarehouse(warehouseId);
    }

    @Transactional
    public InventoryTransaction stockIn(InventoryTransaction tx, User requestingUser) {
        if (tx.getQuantity() <= 0) throw new IllegalArgumentException("Miktar sıfırdan büyük olmalıdır.");
        if (tx.getWarehouse() == null) throw new IllegalArgumentException("Depo belirtilmelidir.");
        if (requestingUser.getRole() == Role.STAFF) {
            log.warn("Yetkisiz erişim: kullanıcı={}, işlem=stok-giriş", requestingUser.getUsername());
            throw new SecurityException("STAFF rolü doğrudan stok girişi yapamaz.");
        }
        if (requestingUser.getRole() != Role.ADMIN) {
            if (requestingUser.getWarehouse() == null) {
                log.warn("Yetkisiz erişim: kullanıcı={}, sebep=depo-yok", requestingUser.getUsername());
                throw new SecurityException("Kullanıcının atanmış bir deposu yok.");
            }
            if (requestingUser.getWarehouse().getId() != tx.getWarehouse().getId()) {
                log.warn("Yetkisiz erişim: kullanıcı={}, kendi-depo={}, istenen-depo={}", requestingUser.getUsername(), requestingUser.getWarehouse().getId(), tx.getWarehouse().getId());
                throw new SecurityException("Yalnızca kendi deponuza stok girişi yapabilirsiniz.");
            }
        }
        Product product = productRepository.findById(tx.getProduct().getId())
                .orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı."));
        boolean wasLow = product.getQuantityInStock() <= AppConstants.LOW_STOCK_THRESHOLD;
        product.setQuantityInStock(product.getQuantityInStock() + tx.getQuantity());
        productRepository.save(product);
        tx.setProduct(product);
        tx.setType(TransactionType.IN);
        tx.setTransactionDate(LocalDateTime.now());
        InventoryTransaction saved = transactionRepository.save(tx);
        log.info("Stok IN: ürün={}, miktar={}, depo={}", product.getName(), tx.getQuantity(), tx.getWarehouse().getId());

        if (wasLow && product.getQuantityInStock() > AppConstants.LOW_STOCK_THRESHOLD) {
            String msg = product.getName() + " stoğu normale döndü (" + product.getQuantityInStock() + " adet)";
            userRepository.findByRole(Role.ADMIN).forEach(a ->
                    notificationService.createNotification(a.getId(), "Stok Normale Döndü", msg, NotificationType.LOW_STOCK, product.getId()));
            if (tx.getWarehouse() != null) {
                userRepository.findByRoleAndWarehouse_Id(Role.MANAGER, tx.getWarehouse().getId())
                        .forEach(m -> notificationService.createNotification(m.getId(), "Stok Normale Döndü", msg, NotificationType.LOW_STOCK, product.getId()));
            }
        }

        return saved;
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
        InventoryTransaction savedOut = transactionRepository.save(tx);
        log.info("Stok OUT: ürün={}, miktar={}, depo={}", product.getName(), tx.getQuantity(), warehouseId);

        if (product.getQuantityInStock() <= AppConstants.LOW_STOCK_THRESHOLD
                && !notificationService.wasLowStockNotifiedRecently(product.getId())) {
            String msg = product.getName() + " stoğu kritik seviyede (" + product.getQuantityInStock() + " adet)";
            userRepository.findByRole(Role.ADMIN).forEach(a ->
                    notificationService.createNotification(a.getId(), "Kritik Stok Uyarısı", msg, NotificationType.LOW_STOCK, product.getId()));
            if (tx.getWarehouse() != null) {
                userRepository.findByRoleAndWarehouse_Id(Role.MANAGER, tx.getWarehouse().getId())
                        .forEach(m -> notificationService.createNotification(m.getId(), "Kritik Stok Uyarısı", msg, NotificationType.LOW_STOCK, product.getId()));
            }
        }

        return savedOut;
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
