package org.example.inventoryapi.repository;

import org.example.inventoryapi.model.entity.AssetTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetTransferRepository extends JpaRepository<AssetTransfer, Integer> {
    List<AssetTransfer> findByAssetIdOrderByTransferDateDesc(int assetId);
}
