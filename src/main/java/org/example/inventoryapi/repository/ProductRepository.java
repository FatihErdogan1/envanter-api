package org.example.inventoryapi.repository;

import org.example.inventoryapi.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    boolean existsBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, int id);
    long countByQuantityInStockLessThanEqual(int threshold);
    @Query("SELECT COUNT(p) FROM Product p JOIN p.suppliers s WHERE s.id = :supplierId")
    int countBySupplierId(@Param("supplierId") int supplierId);
    int countByWarehouseId(int warehouseId);
    long countByWarehouseIdAndQuantityInStockLessThanEqual(int warehouseId, int threshold);
    int countByCategoryId(int categoryId);
    @Query("SELECT p FROM Product p JOIN p.suppliers s WHERE s.id = :supplierId")
    List<Product> findBySupplierId(@Param("supplierId") int supplierId);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.quantityInStock = p.quantityInStock + :delta WHERE p.id = :id")
    void incrementStock(@Param("id") int id, @Param("delta") int delta);
}
