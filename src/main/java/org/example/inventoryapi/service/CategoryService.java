package org.example.inventoryapi.service;

import org.example.inventoryapi.model.entity.Category;
import org.example.inventoryapi.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
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
        categoryRepository.deleteById(id);
    }
}
