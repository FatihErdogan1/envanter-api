package org.example.inventoryapi.service;

import org.example.inventoryapi.model.entity.InventoryTransaction;
import org.example.inventoryapi.model.entity.Product;
import org.example.inventoryapi.model.entity.StockRequest;
import org.example.inventoryapi.model.entity.User;
import org.example.inventoryapi.model.entity.Warehouse;
import org.example.inventoryapi.model.enums.NotificationType;
import org.example.inventoryapi.model.enums.Role;
import org.example.inventoryapi.model.enums.StockRequestStatus;
import org.example.inventoryapi.model.enums.TransactionType;
import org.example.inventoryapi.repository.InventoryTransactionRepository;
import org.example.inventoryapi.repository.ProductRepository;
import org.example.inventoryapi.repository.StockRequestRepository;
import org.example.inventoryapi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockRequestService {

    private final StockRequestRepository stockRequestRepository;
    private final ProductRepository productRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public StockRequestService(StockRequestRepository stockRequestRepository,
                               ProductRepository productRepository,
                               InventoryTransactionRepository transactionRepository,
                               UserRepository userRepository,
                               NotificationService notificationService) {
        this.stockRequestRepository = stockRequestRepository;
        this.productRepository = productRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public List<StockRequest> getRequestsFor(User user) {
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER) {
            return stockRequestRepository.findAllByOrderByRequestDateDesc();
        }
        return stockRequestRepository.findByRequestedByUsernameOrderByRequestDateDesc(user.getUsername());
    }

    public StockRequest create(Product product, Warehouse warehouse, int quantity, String notes, User requester) {
        if (quantity <= 0) throw new IllegalArgumentException("Miktar sıfırdan büyük olmalıdır.");
        StockRequest request = new StockRequest();
        request.setProduct(product);
        request.setWarehouse(warehouse);
        request.setQuantity(quantity);
        request.setNotes(notes);
        request.setRequestedBy(requester);
        request.setStatus(StockRequestStatus.PENDING);
        request.setRequestDate(LocalDateTime.now());
        StockRequest saved = stockRequestRepository.save(request);

        String msg = product.getName() + " için " + quantity + " adet stok talebi";
        userRepository.findByRoleAndWarehouse_Id(Role.MANAGER, warehouse.getId())
                .forEach(manager -> notificationService.createNotification(
                        manager.getId(), "Yeni Stok Talebi", msg,
                        NotificationType.STOCK_REQUEST, saved.getId()));

        return saved;
    }

    @Transactional
    public StockRequest approve(int id, User reviewer, String managerNote) {
        StockRequest request = getPending(id);
        Product product = productRepository.findById(request.getProduct().getId())
                .orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı."));
        product.setQuantityInStock(product.getQuantityInStock() + request.getQuantity());
        productRepository.save(product);

        InventoryTransaction tx = new InventoryTransaction();
        tx.setProduct(product);
        tx.setWarehouse(request.getWarehouse());
        tx.setUser(reviewer);
        tx.setType(TransactionType.IN);
        tx.setQuantity(request.getQuantity());
        tx.setNotes("Talep onayı #" + request.getId() + (managerNote == null || managerNote.isBlank() ? "" : " - " + managerNote));
        tx.setTransactionDate(LocalDateTime.now());
        transactionRepository.save(tx);

        request.setStatus(StockRequestStatus.APPROVED);
        request.setReviewedBy(reviewer);
        request.setManagerNote(managerNote);
        request.setReviewDate(LocalDateTime.now());
        StockRequest saved = stockRequestRepository.save(request);

        if (request.getRequestedBy().getId() != reviewer.getId()) {
            notificationService.createNotification(
                    request.getRequestedBy().getId(),
                    "Stok Talebiniz Onaylandı",
                    product.getName() + " talebi onaylandı",
                    NotificationType.STOCK_REQUEST, id);
        }

        return saved;
    }

    public StockRequest reject(int id, User reviewer, String managerNote) {
        StockRequest request = getPending(id);
        request.setStatus(StockRequestStatus.REJECTED);
        request.setReviewedBy(reviewer);
        request.setManagerNote(managerNote);
        request.setReviewDate(LocalDateTime.now());
        StockRequest saved = stockRequestRepository.save(request);

        if (request.getRequestedBy().getId() != reviewer.getId()) {
            notificationService.createNotification(
                    request.getRequestedBy().getId(),
                    "Stok Talebiniz Reddedildi",
                    request.getProduct().getName() + " talebi reddedildi",
                    NotificationType.STOCK_REQUEST, id);
        }

        return saved;
    }

    private StockRequest getPending(int id) {
        StockRequest request = stockRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Talep bulunamadı."));
        if (request.getStatus() != StockRequestStatus.PENDING)
            throw new IllegalStateException("Bu talep zaten sonuçlandırılmış.");
        return request;
    }
}
