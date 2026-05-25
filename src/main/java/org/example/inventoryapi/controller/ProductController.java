package org.example.inventoryapi.controller;

import org.example.inventoryapi.dto.PageResponse;
import org.example.inventoryapi.model.entity.Product;
import org.example.inventoryapi.model.entity.Supplier;
import org.example.inventoryapi.model.entity.User;
import org.example.inventoryapi.model.enums.Role;
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
    public PageResponse<Product> getAll(
            @AuthenticationPrincipal String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = getUser(username);
        List<Product> products = productService.getAllProducts();
        if (user.getRole() == Role.MANAGER) {
            if (user.getWarehouse() == null) return PageResponse.of(List.of(), page, size);
            int warehouseId = user.getWarehouse().getId();
            products = products.stream()
                    .filter(p -> p.getWarehouse() != null && p.getWarehouse().getId() == warehouseId)
                    .toList();
        }
        return PageResponse.of(products, page, size);
    }

    @GetMapping("/{id}")
    public Product getById(@PathVariable int id) {
        return productService.getProduct(id);
    }

    @PostMapping
    public Product add(@AuthenticationPrincipal String username, @RequestBody Product product) {
        return productService.addProduct(product, getUser(username));
    }

    @PutMapping("/{id}")
    public Product update(@AuthenticationPrincipal String username, @PathVariable int id, @RequestBody Product product) {
        return productService.updateProduct(id, product, getUser(username));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@AuthenticationPrincipal String username, @PathVariable int id) {
        productService.deleteProduct(id, getUser(username));
        return ResponseEntity.ok("Ürün silindi.");
    }

    @GetMapping("/{id}/stock-summary")
    public List<?> stockSummary(@PathVariable int id) {
        return productService.getProductStockSummary(id);
    }

    @GetMapping("/{id}/suppliers")
    public List<Supplier> suppliers(@PathVariable int id) {
        return productService.getProductSuppliers(id);
    }

    @GetMapping("/{id}/transaction-history")
    public List<?> transactionHistory(@PathVariable int id) {
        return productService.getProductTransactionHistory(id);
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
    }
}
