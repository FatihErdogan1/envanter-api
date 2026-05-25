package org.example.inventoryapi.controller;

import org.example.inventoryapi.model.entity.Asset;
import org.example.inventoryapi.model.entity.User;
import org.example.inventoryapi.model.enums.AssetStatus;
import org.example.inventoryapi.model.enums.Role;
import org.example.inventoryapi.repository.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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
    public Map<String, Object> getStats(@AuthenticationPrincipal String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));

        if (user.getRole() == Role.MANAGER) {
            if (user.getWarehouse() == null) {
                return Map.of(
                    "totalUsers", 0L, "totalProducts", 0L, "totalAssets", 0L,
                    "totalWarehouses", 0L, "availableAssets", 0L, "inUseAssets", 0L,
                    "maintenanceAssets", 0L, "retiredAssets", 0L, "criticalStockCount", 0L
                );
            }
            int warehouseId = user.getWarehouse().getId();
            List<Asset> warehouseAssets = assetRepository.findByWarehouse_Id(warehouseId);
            return Map.of(
                "totalUsers",         0L,
                "totalProducts",      (long) productRepository.countByWarehouseId(warehouseId),
                "totalAssets",        (long) assetRepository.countByWarehouseId(warehouseId),
                "totalWarehouses",    1L,
                "availableAssets",    warehouseAssets.stream().filter(a -> a.getStatus() == AssetStatus.AVAILABLE).count(),
                "inUseAssets",        warehouseAssets.stream().filter(a -> a.getStatus() == AssetStatus.IN_USE).count(),
                "maintenanceAssets",  warehouseAssets.stream().filter(a -> a.getStatus() == AssetStatus.MAINTENANCE).count(),
                "retiredAssets",      warehouseAssets.stream().filter(a -> a.getStatus() == AssetStatus.RETIRED).count(),
                "criticalStockCount", productRepository.countByWarehouseIdAndQuantityInStockLessThan(warehouseId, 10)
            );
        }

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

        long criticalStockCount = productRepository.countByQuantityInStockLessThan(10);

        return Map.of(
            "totalUsers",         totalUsers,
            "totalProducts",      totalProducts,
            "totalAssets",        totalAssets,
            "totalWarehouses",    totalWarehouses,
            "availableAssets",    availableAssets,
            "inUseAssets",        inUseAssets,
            "maintenanceAssets",  maintenanceAssets,
            "retiredAssets",      retiredAssets,
            "criticalStockCount", criticalStockCount
        );
    }
}
