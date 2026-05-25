package org.example.inventoryapi.controller;

import org.example.inventoryapi.dto.PageResponse;
import org.example.inventoryapi.model.entity.*;
import org.example.inventoryapi.model.enums.Role;
import org.example.inventoryapi.repository.UserRepository;
import org.example.inventoryapi.repository.WarehouseRepository;
import org.example.inventoryapi.service.AssetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService        assetService;
    private final UserRepository      userRepository;
    private final WarehouseRepository warehouseRepository;

    public AssetController(AssetService assetService, UserRepository userRepository,
                           WarehouseRepository warehouseRepository) {
        this.assetService        = assetService;
        this.userRepository      = userRepository;
        this.warehouseRepository = warehouseRepository;
    }

    @GetMapping
    public List<Asset> getAll(@AuthenticationPrincipal String username) {
        return assetService.getAllAssets(getUser(username));
    }

    @PostMapping
    public Asset add(@AuthenticationPrincipal String username, @RequestBody Asset asset) {
        return assetService.addAsset(asset, getUser(username));
    }

    @PutMapping("/{id}")
    public Asset update(@PathVariable int id, @RequestBody Asset asset) {
        return assetService.updateAsset(id, asset);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable int id) {
        assetService.deleteAsset(id);
        return ResponseEntity.ok("Demirbaş silindi.");
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<String> assign(@AuthenticationPrincipal String username,
                                         @PathVariable int id,
                                         @RequestBody Map<String, Object> body) {
        int targetUserId = (int) body.get("userId");
        String notes = (String) body.getOrDefault("notes", "");
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
        assetService.assignAsset(id, getUser(username), targetUser, notes);
        return ResponseEntity.ok("Demirbaş zimmetlendi.");
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<String> returnAsset(@AuthenticationPrincipal String username,
                                               @PathVariable int id) {
        assetService.returnAsset(id, getUser(username));
        return ResponseEntity.ok("Zimmet düşürüldü.");
    }

    @PostMapping("/{id}/retire")
    public ResponseEntity<String> retire(@AuthenticationPrincipal String username,
                                          @PathVariable int id) {
        assetService.retireAsset(id, getUser(username));
        return ResponseEntity.ok("Demirbaş hurdaya ayrıldı.");
    }

    @PostMapping("/{id}/transfer")
    public ResponseEntity<String> transfer(@AuthenticationPrincipal String username,
                                            @PathVariable int id,
                                            @RequestBody Map<String, Object> body) {
        int warehouseId = (int) body.get("warehouseId");
        String notes = (String) body.getOrDefault("notes", "");
        Warehouse to = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Depo bulunamadı."));
        assetService.transferAsset(id, to, notes, getUser(username));
        return ResponseEntity.ok("Transfer tamamlandı.");
    }

    @PostMapping("/{id}/maintenance/start")
    public ResponseEntity<String> startMaintenance(@AuthenticationPrincipal String username,
                                                    @PathVariable int id,
                                                    @RequestBody Map<String, String> body) {
        assetService.startMaintenance(id, body.get("description"), body.getOrDefault("notes", ""), getUser(username));
        return ResponseEntity.ok("Demirbaş bakıma alındı.");
    }

    @PostMapping("/{id}/maintenance/end")
    public ResponseEntity<String> endMaintenance(@AuthenticationPrincipal String username,
                                                  @PathVariable int id) {
        assetService.endMaintenance(id, getUser(username));
        return ResponseEntity.ok("Bakım tamamlandı.");
    }

    @GetMapping("/{id}/assignments")
    public List<AssetAssignment> assignments(@PathVariable int id) {
        return assetService.getAssignmentHistory(id);
    }

    @GetMapping("/{id}/transfers")
    public List<AssetTransfer> transfers(@PathVariable int id) {
        return assetService.getTransferHistory(id);
    }

    @GetMapping("/{id}/maintenance")
    public List<AssetMaintenance> maintenance(@PathVariable int id) {
        return assetService.getMaintenanceHistory(id);
    }

    @GetMapping("/warehouse-summary")
    public ResponseEntity<?> warehouseSummary(@AuthenticationPrincipal String username) {
        User user = getUser(username);
        if (user.getRole() == Role.MANAGER) {
            if (user.getWarehouse() == null) return ResponseEntity.ok(List.of());
            return ResponseEntity.ok(assetService.getWarehouseStatusSummaryById(user.getWarehouse().getId()));
        }
        return ResponseEntity.ok(assetService.getWarehouseStatusSummary());
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
    }
}
