package org.example.inventoryapi.service;

import org.example.inventoryapi.constants.AppConstants;
import org.example.inventoryapi.model.entity.*;
import org.example.inventoryapi.model.enums.NotificationType;
import org.example.inventoryapi.model.enums.Role;
import org.example.inventoryapi.model.enums.SupplierOrderStatus;
import org.example.inventoryapi.model.enums.TransactionType;
import org.example.inventoryapi.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SupplierOrderService {

    private static final Logger log = LoggerFactory.getLogger(SupplierOrderService.class);

    private final SupplierOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public SupplierOrderService(SupplierOrderRepository orderRepository,
                                ProductRepository productRepository,
                                SupplierRepository supplierRepository,
                                WarehouseRepository warehouseRepository,
                                InventoryTransactionRepository transactionRepository,
                                UserRepository userRepository,
                                NotificationService notificationService) {
        this.orderRepository     = orderRepository;
        this.productRepository   = productRepository;
        this.supplierRepository  = supplierRepository;
        this.warehouseRepository = warehouseRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository      = userRepository;
        this.notificationService = notificationService;
    }

    public List<SupplierOrder> getAllOrders() {
        return orderRepository.findAllByOrderByOrderDateDesc();
    }

    public List<SupplierOrder> getOrdersBySupplier(int supplierId) {
        return orderRepository.findBySupplier_IdOrderByOrderDateDesc(supplierId);
    }

    @Transactional
    public SupplierOrder createOrder(int productId, int supplierId, int warehouseId, int quantity, String createdByUsername) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı."));
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Tedarikçi bulunamadı."));
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Depo bulunamadı."));

        boolean supplierSellsProduct = product.getSuppliers().stream()
                .anyMatch(s -> s.getId() == supplierId);
        if (!supplierSellsProduct)
            throw new IllegalArgumentException("Bu ürün seçilen tedarikçiye ait değil.");
        if (quantity <= 0)
            throw new IllegalArgumentException("Miktar sıfırdan büyük olmalıdır.");

        User createdBy = userRepository.findByUsername(createdByUsername).orElse(null);
        SupplierOrder order = new SupplierOrder();
        order.setProduct(product);
        order.setSupplier(supplier);
        order.setWarehouse(warehouse);
        order.setCreatedBy(createdBy);
        order.setQuantity(quantity);
        order.setStatus(SupplierOrderStatus.BEKLIYOR);
        order.setOrderDate(LocalDateTime.now());
        SupplierOrder saved = orderRepository.save(order);

        String orderMsg = product.getName() + " için " + quantity + " adet sipariş";
        List<User> supplierUsers = userRepository.findByRoleAndSupplier_Id(Role.SUPPLIER, supplierId);
        log.info("[Order] Sipariş #{} oluşturuldu: supplierId={}, SUPPLIER kullanıcı sayısı={}",
                saved.getId(), supplierId, supplierUsers.size());
        for (User su : supplierUsers) {
            log.info("[Order] SUPPLIER bildirimi: orderId={}, userId={}, username={}", saved.getId(), su.getId(), su.getUsername());
            notificationService.createNotification(su.getId(), "Yeni Sipariş", orderMsg, NotificationType.ORDER, saved.getId());
        }

        return saved;
    }

    @Transactional
    public SupplierOrder updateStatus(int orderId, SupplierOrderStatus newStatus, String requesterUsername) {
        SupplierOrder order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş bulunamadı."));
        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));

        SupplierOrderStatus previousStatus = order.getStatus();
        validateStatusTransition(order, newStatus, requester);

        order.setStatus(newStatus);
        order.setUpdatedDate(LocalDateTime.now());
        SupplierOrder saved = orderRepository.save(order);
        log.info("Sipariş {} → {}: sipariş={}", previousStatus, newStatus, orderId);

        dispatchOrderNotification(saved, newStatus, requester);

        if (newStatus == SupplierOrderStatus.TESLIM_ALINDI) {
            applyStockIn(saved, requester);
        }
        return saved;
    }

    public List<Product> getProductsBySupplier(int supplierId) {
        supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Tedarikçi bulunamadı."));
        return productRepository.findBySupplierId(supplierId);
    }

    @Transactional
    public Product updateProductPrice(int productId, java.math.BigDecimal newPrice, int supplierId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı."));
        boolean owns = product.getSuppliers().stream().anyMatch(s -> s.getId() == supplierId);
        if (!owns) throw new IllegalArgumentException("Bu ürün size ait değil.");
        if (newPrice == null || newPrice.signum() < 0)
            throw new IllegalArgumentException("Fiyat sıfırdan küçük olamaz.");
        product.setPrice(newPrice);
        return productRepository.save(product);
    }

    public List<InventoryTransaction> getTransactionsBySupplier(int supplierId) {
        List<Product> products = getProductsBySupplier(supplierId);
        if (products.isEmpty()) return List.of();
        List<Integer> productIds = products.stream().map(Product::getId).toList();
        return transactionRepository.findByProduct_IdInOrderByTransactionDateDesc(productIds);
    }

    private void validateStatusTransition(SupplierOrder order, SupplierOrderStatus next, User requester) {
        SupplierOrderStatus current = order.getStatus();
        Role role = requester.getRole();
        switch (next) {
            case ONAYLANDI, REDDEDILDI -> {
                if (role != Role.ADMIN && role != Role.SUPPLIER)
                    throw new IllegalStateException("Yalnızca tedarikçi veya admin onaylayabilir/reddedebilir.");
                if (role == Role.SUPPLIER) checkSupplierOwnership(order, requester);
                if (current != SupplierOrderStatus.BEKLIYOR)
                    throw new IllegalStateException("Yalnızca BEKLIYOR durumundaki siparişler işleme alınabilir.");
            }
            case YOLDA -> {
                if (role != Role.ADMIN && role != Role.SUPPLIER)
                    throw new IllegalStateException("Yalnızca tedarikçi veya admin yolda durumuna geçirebilir.");
                if (role == Role.SUPPLIER) checkSupplierOwnership(order, requester);
                if (current != SupplierOrderStatus.ONAYLANDI)
                    throw new IllegalStateException("Yalnızca ONAYLANDI durumundaki siparişler yola çıkarılabilir.");
            }
            case TESLIM_ALINDI -> {
                if (role != Role.ADMIN && role != Role.MANAGER)
                    throw new IllegalStateException("Yalnızca yönetici teslim alabilir.");
                if (current != SupplierOrderStatus.YOLDA)
                    throw new IllegalStateException("Yalnızca YOLDA durumundaki siparişler teslim alınabilir.");
            }
            default -> throw new IllegalStateException("Geçersiz durum geçişi.");
        }
    }

    private void dispatchOrderNotification(SupplierOrder order, SupplierOrderStatus newStatus, User requester) {
        String productName = order.getProduct().getName();
        int orderId = order.getId();
        switch (newStatus) {
            case ONAYLANDI -> {
                if (requester.getRole() == Role.SUPPLIER) {
                    String msg = productName + " siparişi onaylandı";
                    userRepository.findByRole(Role.ADMIN).forEach(a ->
                            notificationService.createNotification(a.getId(), "Sipariş Onaylandı", msg, NotificationType.ORDER, orderId));
                }
            }
            case REDDEDILDI -> {
                if (requester.getRole() == Role.SUPPLIER) {
                    String msg = productName + " siparişi reddedildi";
                    userRepository.findByRole(Role.ADMIN).forEach(a ->
                            notificationService.createNotification(a.getId(), "Sipariş Reddedildi", msg, NotificationType.ORDER, orderId));
                }
            }
            case YOLDA -> {
                if (requester.getRole() == Role.SUPPLIER) {
                    String msg = productName + " siparişi yola çıktı";
                    userRepository.findByRole(Role.ADMIN).forEach(a ->
                            notificationService.createNotification(a.getId(), "Sipariş Yolda", msg, NotificationType.ORDER, orderId));
                    if (order.getWarehouse() != null) {
                        userRepository.findByRoleAndWarehouse_Id(Role.MANAGER, order.getWarehouse().getId())
                                .forEach(m -> notificationService.createNotification(m.getId(), "Sipariş Yolda", msg, NotificationType.ORDER, orderId));
                    }
                }
            }
            case TESLIM_ALINDI -> {
                if (order.getCreatedBy() != null
                        && order.getCreatedBy().getId() != requester.getId()) {
                    notificationService.createNotification(
                            order.getCreatedBy().getId(), "Sipariş Teslim Alındı",
                            productName + " teslim alındı, stok güncellendi",
                            NotificationType.ORDER, orderId);
                }
            }
            default -> { }
        }
    }

    private void checkSupplierOwnership(SupplierOrder order, User requester) {
        if (requester.getSupplier() == null
                || requester.getSupplier().getId() != order.getSupplier().getId()) {
            throw new IllegalStateException("Bu sipariş size ait değil.");
        }
    }

    private void applyStockIn(SupplierOrder order, User requester) {
        Product product = order.getProduct();
        boolean wasLow = product.getQuantityInStock() <= AppConstants.LOW_STOCK_THRESHOLD;

        InventoryTransaction tx = new InventoryTransaction();
        tx.setProduct(product);
        tx.setWarehouse(order.getWarehouse());
        tx.setUser(requester);
        tx.setType(TransactionType.IN);
        tx.setQuantity(order.getQuantity());
        tx.setNotes("Tedarikçi siparişi teslim alındı: #" + order.getId());
        tx.setTransactionDate(LocalDateTime.now());
        transactionRepository.save(tx);

        productRepository.incrementStock(product.getId(), order.getQuantity());

        if (wasLow) {
            int newStock = product.getQuantityInStock() + order.getQuantity();
            if (newStock > AppConstants.LOW_STOCK_THRESHOLD) {
                String msg = product.getName() + " stoğu normale döndü (" + newStock + " adet)";
                userRepository.findByRole(Role.ADMIN).forEach(a ->
                        notificationService.createNotification(a.getId(), "Stok Normale Döndü", msg, NotificationType.LOW_STOCK, product.getId()));
                if (order.getWarehouse() != null) {
                    userRepository.findByRoleAndWarehouse_Id(Role.MANAGER, order.getWarehouse().getId())
                            .forEach(m -> notificationService.createNotification(m.getId(), "Stok Normale Döndü", msg, NotificationType.LOW_STOCK, product.getId()));
                }
            }
        }
    }

}
