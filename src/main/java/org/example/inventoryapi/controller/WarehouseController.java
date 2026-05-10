package org.example.inventoryapi.controller;

import org.example.inventoryapi.dto.WarehouseStockItem;
import org.example.inventoryapi.model.entity.Warehouse;
import org.example.inventoryapi.service.InventoryService;
import org.example.inventoryapi.service.WarehouseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final InventoryService inventoryService;

    public WarehouseController(WarehouseService warehouseService, InventoryService inventoryService) {
        this.warehouseService  = warehouseService;
        this.inventoryService  = inventoryService;
    }

    @GetMapping
    public List<Warehouse> getAll() { return warehouseService.getAllWarehouses(); }

    @PostMapping
    public ResponseEntity<?> add(@RequestBody Warehouse warehouse) {
        try { return ResponseEntity.ok(warehouseService.addWarehouse(warehouse)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody Warehouse warehouse) {
        try { return ResponseEntity.ok(warehouseService.updateWarehouse(id, warehouse)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id) {
        try { warehouseService.deleteWarehouse(id); return ResponseEntity.ok("Depo silindi."); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @GetMapping("/{id}/stock")
    public List<WarehouseStockItem> getStock(@PathVariable int id) {
        return inventoryService.getWarehouseStock(id);
    }
}
