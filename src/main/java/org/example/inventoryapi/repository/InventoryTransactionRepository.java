package org.example.inventoryapi.repository;

import org.example.inventoryapi.dto.WarehouseStockItem;
import org.example.inventoryapi.dto.ProductWarehouseStock;
import org.example.inventoryapi.model.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Integer> {
    List<InventoryTransaction> findAllByOrderByTransactionDateDesc();

    @Query(value = """
            SELECT p.product_id  AS productId,
                   p.name        AS productName,
                   p.sku         AS sku,
                   COALESCE(SUM(
                       CASE
                           WHEN t.transaction_type = 'IN'       AND t.warehouse_id             = :wid THEN  t.quantity
                           WHEN t.transaction_type = 'OUT'      AND t.warehouse_id             = :wid THEN -t.quantity
                           WHEN t.transaction_type = 'TRANSFER' AND t.warehouse_id             = :wid THEN -t.quantity
                           WHEN t.transaction_type = 'TRANSFER' AND t.destination_warehouse_id = :wid THEN  t.quantity
                           ELSE 0
                       END
                   ), 0) AS quantity
            FROM Products p
            LEFT JOIN Inventory_Transactions t
                   ON t.product_id = p.product_id
                  AND (t.warehouse_id = :wid OR t.destination_warehouse_id = :wid)
            GROUP BY p.product_id, p.name, p.sku
            HAVING quantity > 0
            ORDER BY p.name
            """, nativeQuery = true)
    List<WarehouseStockItem> findStockByWarehouse(@Param("wid") int warehouseId);

    @Query(value = """
            SELECT COALESCE(SUM(
                CASE
                    WHEN transaction_type = 'IN'       AND warehouse_id             = :wid THEN  quantity
                    WHEN transaction_type = 'OUT'      AND warehouse_id             = :wid THEN -quantity
                    WHEN transaction_type = 'TRANSFER' AND warehouse_id             = :wid THEN -quantity
                    WHEN transaction_type = 'TRANSFER' AND destination_warehouse_id = :wid THEN  quantity
                    ELSE 0
                END
            ), 0)
            FROM Inventory_Transactions
            WHERE product_id = :pid
              AND (warehouse_id = :wid OR destination_warehouse_id = :wid)
            """, nativeQuery = true)
    long getProductStockInWarehouse(@Param("pid") int productId, @Param("wid") int warehouseId);

    @Query(value = """
            SELECT w.warehouse_id AS warehouseId,
                   w.name         AS warehouseName,
                   COALESCE(SUM(
                       CASE
                           WHEN t.transaction_type = 'IN'       AND t.warehouse_id             = w.warehouse_id THEN  t.quantity
                           WHEN t.transaction_type = 'OUT'      AND t.warehouse_id             = w.warehouse_id THEN -t.quantity
                           WHEN t.transaction_type = 'TRANSFER' AND t.warehouse_id             = w.warehouse_id THEN -t.quantity
                           WHEN t.transaction_type = 'TRANSFER' AND t.destination_warehouse_id = w.warehouse_id THEN  t.quantity
                           ELSE 0
                       END
                   ), 0) AS quantity
            FROM Warehouses w
            LEFT JOIN Inventory_Transactions t
                   ON t.product_id = :pid
                  AND (t.warehouse_id = w.warehouse_id OR t.destination_warehouse_id = w.warehouse_id)
            GROUP BY w.warehouse_id, w.name
            HAVING quantity > 0
            ORDER BY w.name
            """, nativeQuery = true)
    List<ProductWarehouseStock> findStockByProduct(@Param("pid") int productId);

    @Query(value = """
            SELECT COALESCE(SUM(
                CASE
                    WHEN transaction_type = 'IN'       AND warehouse_id             = :wid THEN  quantity
                    WHEN transaction_type = 'OUT'      AND warehouse_id             = :wid THEN -quantity
                    WHEN transaction_type = 'TRANSFER' AND warehouse_id             = :wid THEN -quantity
                    WHEN transaction_type = 'TRANSFER' AND destination_warehouse_id = :wid THEN  quantity
                    ELSE 0
                END
            ), 0)
            FROM Inventory_Transactions
            """, nativeQuery = true)
    long getTotalStockInWarehouse(@Param("wid") int warehouseId);
}
