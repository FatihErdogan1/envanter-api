package org.example.inventoryapi.controller;

import org.example.inventoryapi.model.entity.Product;
import org.example.inventoryapi.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<Product> getAll() { return productService.getAllProducts(); }

    @PostMapping
    public ResponseEntity<?> add(@RequestBody Product product) {
        try { return ResponseEntity.ok(productService.addProduct(product)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody Product product) {
        try { return ResponseEntity.ok(productService.updateProduct(id, product)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id) {
        try { productService.deleteProduct(id); return ResponseEntity.ok("Ürün silindi."); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }
}
