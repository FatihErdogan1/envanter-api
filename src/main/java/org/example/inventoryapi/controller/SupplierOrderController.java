package org.example.inventoryapi.controller;

import org.example.inventoryapi.model.entity.Product;
import org.example.inventoryapi.model.entity.SupplierOrder;
import org.example.inventoryapi.model.entity.User;
import org.example.inventoryapi.model.enums.Role;
import org.example.inventoryapi.model.enums.SupplierOrderStatus;
import org.example.inventoryapi.repository.UserRepository;
import org.example.inventoryapi.service.SupplierOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/supplier-orders")
public class SupplierOrderController {

    private final SupplierOrderService service;
    private final UserRepository userRepository;

    public SupplierOrderController(SupplierOrderService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> getOrders(@AuthenticationPrincipal String username) {
        try {
            User user = getUser(username);
            if (user.getRole() == Role.SUPPLIER) {
                if (user.getSupplier() == null)
                    return ResponseEntity.badRequest().body("Tedarikçi bağlantısı bulunamadı.");
                return ResponseEntity.ok(service.getOrdersBySupplier(user.getSupplier().getId()));
            }
            return ResponseEntity.ok(service.getAllOrders());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal String username,
                                    @RequestBody Map<String, Object> body) {
        try {
            Object pidObj = body.get("productId");
            Object sidObj = body.get("supplierId");
            Object widObj = body.get("warehouseId");
            Object qtyObj = body.get("quantity");
            if (pidObj == null || sidObj == null || widObj == null || qtyObj == null)
                return ResponseEntity.badRequest().body("productId, supplierId, warehouseId ve quantity zorunludur.");
            int productId   = toInt(pidObj);
            int supplierId  = toInt(sidObj);
            int warehouseId = toInt(widObj);
            int quantity    = toInt(qtyObj);
            SupplierOrder order = service.createOrder(productId, supplierId, warehouseId, quantity, username);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<?> approve(@AuthenticationPrincipal String username, @PathVariable int id) {
        return changeStatus(id, SupplierOrderStatus.ONAYLANDI, username);
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<?> reject(@AuthenticationPrincipal String username, @PathVariable int id) {
        return changeStatus(id, SupplierOrderStatus.REDDEDILDI, username);
    }

    @PatchMapping("/{id}/ship")
    public ResponseEntity<?> ship(@AuthenticationPrincipal String username, @PathVariable int id) {
        return changeStatus(id, SupplierOrderStatus.YOLDA, username);
    }

    @PatchMapping("/{id}/deliver")
    public ResponseEntity<?> deliver(@AuthenticationPrincipal String username, @PathVariable int id) {
        return changeStatus(id, SupplierOrderStatus.TESLIM_ALINDI, username);
    }

    @GetMapping("/my-products")
    public ResponseEntity<?> myProducts(@AuthenticationPrincipal String username) {
        try {
            User user = getUser(username);
            if (user.getSupplier() == null)
                return ResponseEntity.badRequest().body("Tedarikçi bağlantısı bulunamadı.");
            List<Product> products = service.getProductsBySupplier(user.getSupplier().getId());
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/my-products/{productId}/price")
    public ResponseEntity<?> updatePrice(@AuthenticationPrincipal String username,
                                         @PathVariable int productId,
                                         @RequestBody Map<String, Object> body) {
        try {
            User user = getUser(username);
            if (user.getSupplier() == null)
                return ResponseEntity.status(403).body("Tedarikçi bağlantısı bulunamadı.");
            Object priceObj = body.get("price");
            if (priceObj == null)
                return ResponseEntity.badRequest().body("price alanı zorunludur.");
            BigDecimal price = new BigDecimal(priceObj.toString());
            Product updated = service.updateProductPrice(productId, price, user.getSupplier().getId());
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/my-transactions")
    public ResponseEntity<?> myTransactions(@AuthenticationPrincipal String username) {
        try {
            User user = getUser(username);
            if (user.getSupplier() == null)
                return ResponseEntity.badRequest().body("Tedarikçi bağlantısı bulunamadı.");
            return ResponseEntity.ok(service.getTransactionsBySupplier(user.getSupplier().getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private ResponseEntity<?> changeStatus(int orderId, SupplierOrderStatus status, String username) {
        try {
            return ResponseEntity.ok(service.updateStatus(orderId, status, username));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
    }

    private int toInt(Object val) {
        if (val instanceof Integer i) return i;
        return Integer.parseInt(val.toString());
    }
}
