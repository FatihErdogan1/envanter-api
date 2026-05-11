package org.example.inventoryapi.controller;

import org.example.inventoryapi.model.entity.InventoryTransaction;
import org.example.inventoryapi.model.entity.User;
import org.example.inventoryapi.repository.UserRepository;
import org.example.inventoryapi.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final UserRepository   userRepository;

    public InventoryController(InventoryService inventoryService, UserRepository userRepository) {
        this.inventoryService = inventoryService;
        this.userRepository   = userRepository;
    }

    @GetMapping("/transactions")
    public List<InventoryTransaction> getAll() { return inventoryService.getAllTransactions(); }

    @PostMapping("/in")
    public ResponseEntity<?> stockIn(@RequestBody InventoryTransaction tx) {
        try { return ResponseEntity.ok(inventoryService.stockIn(tx)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping("/out")
    public ResponseEntity<?> stockOut(@AuthenticationPrincipal String username,
                                      @RequestBody InventoryTransaction tx) {
        try { return ResponseEntity.ok(inventoryService.stockOut(tx, getUser(username))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@AuthenticationPrincipal String username,
                                      @RequestBody InventoryTransaction tx) {
        try { return ResponseEntity.ok(inventoryService.transfer(tx, getUser(username))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
    }
}
