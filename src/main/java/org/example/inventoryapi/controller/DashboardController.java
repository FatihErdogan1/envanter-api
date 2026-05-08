package org.example.inventoryapi.controller;

import org.example.inventoryapi.model.enums.AssetStatus;
import org.example.inventoryapi.repository.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final UserRepository    userRepository;
    private final ProductRepository productRepository;
    private final AssetRepository   assetRepository;
    private final WarehouseRepository warehouseRepository;

    public DashboardController(UserRepository userRepository, ProductRepository productRepository,
                               AssetRepository assetRepository, WarehouseRepository warehouseRepository) {
        this.userRepository      = userRepository;
        this.productRepository   = productRepository;
        this.assetRepository     = assetRepository;
        this.warehouseRepository = warehouseRepository;
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        long totalUsers      = userRepository.count();
        long totalProducts   = productRepository.count();
        long totalAssets     = assetRepository.count();
        long totalWarehouses = warehouseRepository.count();

        long availableAssets    = assetRepository.findAll().stream()
                .filter(a -> a.getStatus() == AssetStatus.AVAILABLE).count();
        long inUseAssets        = assetRepository.findAll().stream()
                .filter(a -> a.getStatus() == AssetStatus.IN_USE).count();
        long maintenanceAssets  = assetRepository.findAll().stream()
                .filter(a -> a.getStatus() == AssetStatus.MAINTENANCE).count();
        long retiredAssets      = assetRepository.findAll().stream()
                .filter(a -> a.getStatus() == AssetStatus.RETIRED).count();

        return Map.of(
            "totalUsers",         totalUsers,
            "totalProducts",      totalProducts,
            "totalAssets",        totalAssets,
            "totalWarehouses",    totalWarehouses,
            "availableAssets",    availableAssets,
            "inUseAssets",        inUseAssets,
            "maintenanceAssets",  maintenanceAssets,
            "retiredAssets",      retiredAssets
        );
    }
}
