package org.example.inventoryapi.service;

import org.example.inventoryapi.model.entity.Warehouse;
import org.example.inventoryapi.repository.AssetRepository;
import org.example.inventoryapi.repository.WarehouseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final AssetRepository     assetRepository;

    public WarehouseService(WarehouseRepository warehouseRepository, AssetRepository assetRepository) {
        this.warehouseRepository = warehouseRepository;
        this.assetRepository     = assetRepository;
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
        int count = assetRepository.countByWarehouseId(id);
        if (count > 0)
            throw new IllegalStateException("Bu depoda " + count + " demirbaş var. Silinemez.");
        warehouseRepository.deleteById(id);
    }
}
