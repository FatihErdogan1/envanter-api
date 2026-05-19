package org.example.inventoryapi.service;

import org.example.inventoryapi.exception.DeletionBlockedException;
import org.example.inventoryapi.model.entity.Category;
import org.example.inventoryapi.repository.CategoryRepository;
import org.example.inventoryapi.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository  productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository  = productRepository;
    }

    public List<Category> getAllCategories() { return categoryRepository.findAll(); }

    public Category addCategory(Category category) {
        if (category.getName() == null || category.getName().isBlank())
            throw new IllegalArgumentException("Kategori adı boş bırakılamaz.");
        if (categoryRepository.existsByNameIgnoreCase(category.getName()))
            throw new IllegalArgumentException("Bu isimde bir kategori zaten kayıtlıdır.");
        return categoryRepository.save(category);
    }

    public Category updateCategory(int id, Category updated) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Kategori bulunamadı."));
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        return categoryRepository.save(existing);
    }

    public void deleteCategory(int id) {
        int count = productRepository.countByCategoryId(id);
        if (count > 0)
            throw new DeletionBlockedException(
                "Bu kategoride " + count + " ürün bulunuyor, önce taşıyın.",
                count, "ürün");
        categoryRepository.deleteById(id);
    }
}
