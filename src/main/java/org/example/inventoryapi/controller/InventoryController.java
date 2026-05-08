package org.example.inventoryapi.controller;

import org.example.inventoryapi.model.entity.InventoryTransaction;
import org.example.inventoryapi.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/transactions")
    public List<InventoryTransaction> getAll() { return inventoryService.getAllTransactions(); }

    @PostMapping("/in")
    public ResponseEntity<?> stockIn(@RequestBody InventoryTransaction tx) {
        try { return ResponseEntity.ok(inventoryService.stockIn(tx)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping("/out")
    public ResponseEntity<?> stockOut(@RequestBody InventoryTransaction tx) {
        try { return ResponseEntity.ok(inventoryService.stockOut(tx)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestBody InventoryTransaction tx) {
        try { return ResponseEntity.ok(inventoryService.transfer(tx)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }
}
