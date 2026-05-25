package org.example.inventoryapi.controller;

import org.example.inventoryapi.dto.DeletionErrorResponse;
import org.example.inventoryapi.dto.WarehouseStockItem;
import org.example.inventoryapi.model.entity.User;
import org.example.inventoryapi.model.entity.Warehouse;
import org.example.inventoryapi.model.enums.Role;
import org.example.inventoryapi.repository.UserRepository;
import org.example.inventoryapi.service.InventoryService;
import org.example.inventoryapi.service.WarehouseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final InventoryService inventoryService;
    private final UserRepository   userRepository;

    public WarehouseController(WarehouseService warehouseService, InventoryService inventoryService,
                               UserRepository userRepository) {
        this.warehouseService = warehouseService;
        this.inventoryService = inventoryService;
        this.userRepository   = userRepository;
    }

    @GetMapping
    public List<Warehouse> getAll() { return warehouseService.getAllWarehouses(); }

    @PostMapping
    public Warehouse add(@RequestBody Warehouse warehouse) {
        return warehouseService.addWarehouse(warehouse);
    }

    @PutMapping("/{id}")
    public Warehouse update(@PathVariable int id, @RequestBody Warehouse warehouse) {
        return warehouseService.updateWarehouse(id, warehouse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable int id) {
        warehouseService.deleteWarehouse(id);
        return ResponseEntity.ok("Depo silindi.");
    }

    @GetMapping("/{id}/stock")
    public List<WarehouseStockItem> getStock(@AuthenticationPrincipal String username, @PathVariable int id) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
        if (user.getRole() == Role.MANAGER) {
            if (user.getWarehouse() == null || user.getWarehouse().getId() != id)
                throw new SecurityException("Bu depoya erişim yetkiniz yok.");
        }
        return inventoryService.getWarehouseStock(id);
    }
}
