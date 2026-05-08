package org.example.inventoryapi.repository;

import org.example.inventoryapi.model.entity.AssetMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface AssetMaintenanceRepository extends JpaRepository<AssetMaintenance, Integer> {

    Optional<AssetMaintenance> findByAssetIdAndEndDateIsNull(int assetId);

    List<AssetMaintenance> findByAssetIdOrderByStartDateDesc(int assetId);

    @Modifying
    @Transactional
    @Query("UPDATE AssetMaintenance m SET m.endDate = CURRENT_TIMESTAMP WHERE m.assetId = :assetId AND m.endDate IS NULL")
    void closeActiveByAssetId(int assetId);
}
