package org.example.inventoryapi.repository;

import org.example.inventoryapi.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    boolean existsBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, int id);
    long countByQuantityInStockLessThan(int threshold);
}
