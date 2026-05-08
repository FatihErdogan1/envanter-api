package org.example.inventoryapi.repository;

import org.example.inventoryapi.model.entity.AssetAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface AssetAssignmentRepository extends JpaRepository<AssetAssignment, Integer> {

    Optional<AssetAssignment> findByAssetIdAndReturnDateIsNull(int assetId);

    List<AssetAssignment> findByAssetIdOrderByAssignedDateDesc(int assetId);

    int countByUserIdAndReturnDateIsNull(int userId);

    @Query("SELECT aa.assetId FROM AssetAssignment aa WHERE aa.user.id = :userId AND aa.returnDate IS NULL")
    List<Integer> findActiveAssetIdsByUserId(int userId);

    @Modifying
    @Transactional
    @Query("UPDATE AssetAssignment aa SET aa.returnDate = CURRENT_TIMESTAMP WHERE aa.user.id = :userId AND aa.returnDate IS NULL")
    void closeAllActiveByUserId(int userId);
}
