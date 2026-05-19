package org.example.inventoryapi.service;

import org.example.inventoryapi.exception.DeletionBlockedException;
import org.example.inventoryapi.model.entity.Warehouse;
import org.example.inventoryapi.repository.AssetRepository;
import org.example.inventoryapi.repository.InventoryTransactionRepository;
import org.example.inventoryapi.repository.ProductRepository;
import org.example.inventoryapi.repository.WarehouseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WarehouseService {

    private final WarehouseRepository             warehouseRepository;
    private final AssetRepository                 assetRepository;
    private final ProductRepository               productRepository;
    private final InventoryTransactionRepository  transactionRepository;

    public WarehouseService(WarehouseRepository warehouseRepository,
                            AssetRepository assetRepository,
                            ProductRepository productRepository,
                            InventoryTransactionRepository transactionRepository) {
        this.warehouseRepository  = warehouseRepository;
        this.assetRepository      = assetRepository;
        this.productRepository    = productRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<Warehouse> getAllWarehouses() { return warehouseRepository.findAll(); }

    public Warehouse addWarehouse(Warehouse warehouse) {
        if (warehouse.getName() == null || warehouse.getName().isBlank())
            throw new IllegalArgumentException("Depo adı boş bırakılamaz.");
        if (warehouseRepository.existsByNameIgnoreCase(warehouse.getName()))
            throw new IllegalArgumentException("Bu isimde bir depo zaten kayıtlıdır.");
        return warehouseRepository.save(warehouse);
    }

    public Warehouse updateWarehouse(int id, Warehouse updated) {
        Warehouse existing = warehouseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Depo bulunamadı."));
        existing.setName(updated.getName());
        existing.setLocationAddress(updated.getLocationAddress());
        return warehouseRepository.save(existing);
    }

    public void deleteWarehouse(int id) {
        long totalStock = transactionRepository.getTotalStockInWarehouse(id);
        if (totalStock > 0)
            throw new DeletionBlockedException(
                "Bu depoda " + totalStock + " adet stok kaydı var, önce boşaltın.",
                (int) totalStock, "stok");

        int assetCount = assetRepository.countByWarehouseId(id);
        if (assetCount > 0)
            throw new DeletionBlockedException(
                "Bu depoda " + assetCount + " demirbaş var. Önce taşıyın.",
                assetCount, "demirbaş");

        int productCount = productRepository.countByWarehouseId(id);
        if (productCount > 0)
            throw new DeletionBlockedException(
                "Bu depoya atanmış " + productCount + " ürün var. Önce kaldırın.",
                productCount, "ürün");

        warehouseRepository.deleteById(id);
    }
}
