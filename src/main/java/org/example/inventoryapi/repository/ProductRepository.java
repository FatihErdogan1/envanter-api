package org.example.inventoryapi.repository;

import org.example.inventoryapi.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    boolean existsBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, int id);
    long countByQuantityInStockLessThan(int threshold);
    @Query("SELECT COUNT(p) FROM Product p JOIN p.suppliers s WHERE s.id = :supplierId")
    int countBySupplierId(@Param("supplierId") int supplierId);
    int countByWarehouseId(int warehouseId);
    int countByCategoryId(int categoryId);
}
