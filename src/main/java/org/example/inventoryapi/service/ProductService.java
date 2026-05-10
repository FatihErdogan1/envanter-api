package org.example.inventoryapi.service;

import org.example.inventoryapi.model.entity.Product;
import org.example.inventoryapi.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() { return productRepository.findAll(); }

    public Product addProduct(Product product) {
        if (product.getSku() == null || product.getSku().isBlank())
            throw new IllegalArgumentException("SKU zorunludur.");
        if (product.getName() == null || product.getName().isBlank())
            throw new IllegalArgumentException("Ürün adı zorunludur.");
        if (productRepository.existsBySkuIgnoreCase(product.getSku()))
            throw new IllegalArgumentException("Bu SKU zaten kayıtlı.");
        if (product.getPrice() != null && product.getPrice().signum() < 0)
            throw new IllegalArgumentException("Fiyat negatif olamaz.");
        if (product.getQuantityInStock() < 0)
            throw new IllegalArgumentException("Stok miktarı negatif olamaz.");
        return productRepository.save(product);
    }

    public Product updateProduct(int id, Product updated) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ürün bulunamadı."));
        if (productRepository.existsBySkuIgnoreCaseAndIdNot(updated.getSku(), id))
            throw new IllegalArgumentException("Bu SKU başka bir ürüne ait.");
        if (updated.getPrice() != null && updated.getPrice().signum() < 0)
            throw new IllegalArgumentException("Fiyat negatif olamaz.");
        if (updated.getQuantityInStock() < 0)
            throw new IllegalArgumentException("Stok miktarı negatif olamaz.");
        existing.setSku(updated.getSku());
        existing.setName(updated.getName());
        existing.setCategory(updated.getCategory());
        existing.setPrice(updated.getPrice());
        existing.setQuantityInStock(updated.getQuantityInStock());
        return productRepository.save(existing);
    }

    public void deleteProduct(int id) {
        productRepository.deleteById(id);
    }
}
