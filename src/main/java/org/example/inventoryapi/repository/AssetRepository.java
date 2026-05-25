package org.example.inventoryapi.repository;

import org.example.inventoryapi.model.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, Integer> {
    boolean existsBySerialNumberIgnoreCase(String serialNumber);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, int id);
    int countBySupplierId(int supplierId);
    int countByWarehouseId(int warehouseId);
    List<Asset> findByWarehouse_Id(int warehouseId);

    @Query("SELECT a.warehouse.name, CAST(a.status AS string), COUNT(a) " +
           "FROM Asset a GROUP BY a.warehouse.name, a.status")
    List<Object[]> getStatusSummaryByWarehouse();

    @Query("SELECT a.warehouse.name, CAST(a.status AS string), COUNT(a) " +
           "FROM Asset a WHERE a.warehouse.id = :warehouseId GROUP BY a.warehouse.name, a.status")
    List<Object[]> getStatusSummaryByWarehouseId(@Param("warehouseId") int warehouseId);
}
