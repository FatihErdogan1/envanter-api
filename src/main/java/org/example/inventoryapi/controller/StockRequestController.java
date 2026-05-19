package org.example.inventoryapi.controller;

import org.example.inventoryapi.model.entity.Product;
import org.example.inventoryapi.model.entity.StockRequest;
import org.example.inventoryapi.model.entity.User;
import org.example.inventoryapi.model.entity.Warehouse;
import org.example.inventoryapi.repository.ProductRepository;
import org.example.inventoryapi.repository.UserRepository;
import org.example.inventoryapi.repository.WarehouseRepository;
import org.example.inventoryapi.service.StockRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stock-requests")
public class StockRequestController {

    private final StockRequestService stockRequestService;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;

    public StockRequestController(StockRequestService stockRequestService,
                                  ProductRepository productRepository,
                                  WarehouseRepository warehouseRepository,
                                  UserRepository userRepository) {
        this.stockRequestService = stockRequestService;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAll(@AuthenticationPrincipal String username) {
        try {
            return ResponseEntity.ok(stockRequestService.getRequestsFor(getUser(username)));
        } catch (SecurityException e) { return ResponseEntity.status(403).body(e.getMessage()); }
          catch (Exception e)          { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal String username,
                                    @RequestBody Map<String, Object> body) {
        try {
            Product product = productRepository.findById(extractId(body.get("product")))
                    .orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı."));
            Warehouse warehouse = warehouseRepository.findById(extractId(body.get("warehouse")))
                    .orElseThrow(() -> new IllegalArgumentException("Depo bulunamadı."));
            int quantity = ((Number) body.get("quantity")).intValue();
            String notes = (String) body.getOrDefault("notes", "");
            StockRequest request = stockRequestService.create(product, warehouse, quantity, notes, getUser(username));
            return ResponseEntity.ok(request);
        } catch (SecurityException e) { return ResponseEntity.status(403).body(e.getMessage()); }
          catch (Exception e)          { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@AuthenticationPrincipal String username,
                                     @PathVariable int id,
                                     @RequestBody(required = false) Map<String, Object> body) {
        try {
            String managerNote = body == null ? "" : (String) body.getOrDefault("managerNote", "");
            return ResponseEntity.ok(stockRequestService.approve(id, getUser(username), managerNote));
        } catch (SecurityException e) { return ResponseEntity.status(403).body(e.getMessage()); }
          catch (Exception e)          { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@AuthenticationPrincipal String username,
                                    @PathVariable int id,
                                    @RequestBody(required = false) Map<String, Object> body) {
        try {
            String managerNote = body == null ? "" : (String) body.getOrDefault("managerNote", "");
            return ResponseEntity.ok(stockRequestService.reject(id, getUser(username), managerNote));
        } catch (SecurityException e) { return ResponseEntity.status(403).body(e.getMessage()); }
          catch (Exception e)          { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
    }

    private int extractId(Object value) {
        if (value instanceof Number n) return n.intValue();
        if (value instanceof Map<?, ?> map) return ((Number) map.get("id")).intValue();
        return Integer.parseInt(value.toString());
    }
}
