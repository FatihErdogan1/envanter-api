package org.example.inventoryapi.controller;

import org.example.inventoryapi.dto.DeletionErrorResponse;
import org.example.inventoryapi.exception.DeletionBlockedException;
import org.example.inventoryapi.model.entity.Product;
import org.example.inventoryapi.model.entity.User;
import org.example.inventoryapi.repository.UserRepository;
import org.example.inventoryapi.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final UserRepository userRepository;

    public ProductController(ProductService productService, UserRepository userRepository) {
        this.productService = productService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Product> getAll() { return productService.getAllProducts(); }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable int id) {
        try { return ResponseEntity.ok(productService.getProduct(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping
    public ResponseEntity<?> add(@AuthenticationPrincipal String username, @RequestBody Product product) {
        try { return ResponseEntity.ok(productService.addProduct(product, getUser(username))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@AuthenticationPrincipal String username, @PathVariable int id, @RequestBody Product product) {
        try { return ResponseEntity.ok(productService.updateProduct(id, product, getUser(username))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal String username, @PathVariable int id) {
        try { productService.deleteProduct(id, getUser(username)); return ResponseEntity.ok("Ürün silindi."); }
        catch (SecurityException e) { return ResponseEntity.status(403).body(e.getMessage()); }
        catch (DeletionBlockedException e) { return ResponseEntity.status(409).body(DeletionErrorResponse.of(e.getMessage(), e.getCount(), e.getRelatedEntity())); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @GetMapping("/{id}/stock-summary")
    public ResponseEntity<?> stockSummary(@PathVariable int id) {
        try { return ResponseEntity.ok(productService.getProductStockSummary(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @GetMapping("/{id}/suppliers")
    public ResponseEntity<?> suppliers(@PathVariable int id) {
        try { return ResponseEntity.ok(productService.getProductSuppliers(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @GetMapping("/{id}/transaction-history")
    public ResponseEntity<?> transactionHistory(@PathVariable int id) {
        try { return ResponseEntity.ok(productService.getProductTransactionHistory(id)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
    }
}
