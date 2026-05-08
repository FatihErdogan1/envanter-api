package org.example.inventoryapi.controller;

import org.example.inventoryapi.model.entity.Category;
import org.example.inventoryapi.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<Category> getAll() { return categoryService.getAllCategories(); }

    @PostMapping
    public ResponseEntity<?> add(@RequestBody Category category) {
        try { return ResponseEntity.ok(categoryService.addCategory(category)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody Category category) {
        try { return ResponseEntity.ok(categoryService.updateCategory(id, category)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id) {
        try { categoryService.deleteCategory(id); return ResponseEntity.ok("Kategori silindi."); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }
}
