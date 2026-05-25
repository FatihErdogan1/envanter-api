package org.example.inventoryapi.controller;

import org.example.inventoryapi.dto.PageResponse;
import org.example.inventoryapi.model.entity.InventoryTransaction;
import org.example.inventoryapi.model.entity.User;
import org.example.inventoryapi.repository.UserRepository;
import org.example.inventoryapi.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    public PageResponse<InventoryTransaction> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(inventoryService.getAllTransactions(), page, size);
    }

    @PostMapping("/in")
    public InventoryTransaction stockIn(@AuthenticationPrincipal String username,
                                        @RequestBody InventoryTransaction tx) {
        return inventoryService.stockIn(tx, getUser(username));
    }

    @PostMapping("/out")
    public InventoryTransaction stockOut(@AuthenticationPrincipal String username,
                                          @RequestBody InventoryTransaction tx) {
        return inventoryService.stockOut(tx, getUser(username));
    }

    @PostMapping("/transfer")
    public InventoryTransaction transfer(@AuthenticationPrincipal String username,
                                          @RequestBody InventoryTransaction tx) {
        return inventoryService.transfer(tx, getUser(username));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
    }
}
