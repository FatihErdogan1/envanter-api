package org.example.inventoryapi.controller;

import org.example.inventoryapi.dto.DeletionErrorResponse;
import org.example.inventoryapi.exception.DeletionBlockedException;
import org.example.inventoryapi.model.entity.*;
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
    public ResponseEntity<?> getAll(@AuthenticationPrincipal String username) {
        try {
            User user = getUser(username);
            return ResponseEntity.ok(assetService.getAllAssets(user));
        } catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping
    public ResponseEntity<?> add(@AuthenticationPrincipal String username, @RequestBody Asset asset) {
        try {
            User requestingUser = getUser(username);
            return ResponseEntity.ok(assetService.addAsset(asset, requestingUser));
        } catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody Asset asset) {
        try { return ResponseEntity.ok(assetService.updateAsset(id, asset)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id) {
        try { assetService.deleteAsset(id); return ResponseEntity.ok("Demirbaş silindi."); }
        catch (DeletionBlockedException e) { return ResponseEntity.status(409).body(DeletionErrorResponse.of(e.getMessage(), e.getCount(), e.getRelatedEntity())); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<?> assign(@AuthenticationPrincipal String username,
                                    @PathVariable int id,
                                    @RequestBody Map<String, Object> body) {
        try {
            User requestingUser = getUser(username);
            int targetUserId = (int) body.get("userId");
            String notes = (String) body.getOrDefault("notes", "");
            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
            assetService.assignAsset(id, requestingUser, targetUser, notes);
            return ResponseEntity.ok("Demirbaş zimmetlendi.");
        } catch (SecurityException e) { return ResponseEntity.status(403).body(e.getMessage()); }
          catch (Exception e)          { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<?> returnAsset(@AuthenticationPrincipal String username,
                                         @PathVariable int id) {
        try {
            User requestingUser = getUser(username);
            assetService.returnAsset(id, requestingUser);
            return ResponseEntity.ok("Zimmet düşürüldü.");
        } catch (SecurityException e) { return ResponseEntity.status(403).body(e.getMessage()); }
          catch (Exception e)          { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping("/{id}/retire")
    public ResponseEntity<?> retire(@AuthenticationPrincipal String username,
                                    @PathVariable int id) {
        try {
            assetService.retireAsset(id, getUser(username));
            return ResponseEntity.ok("Demirbaş hurdaya ayrıldı.");
        } catch (SecurityException e) { return ResponseEntity.status(403).body(e.getMessage()); }
          catch (Exception e)          { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping("/{id}/transfer")
    public ResponseEntity<?> transfer(@AuthenticationPrincipal String username,
                                      @PathVariable int id,
                                      @RequestBody Map<String, Object> body) {
        try {
            int warehouseId = (int) body.get("warehouseId");
            String notes = (String) body.getOrDefault("notes", "");
            Warehouse to = warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new IllegalArgumentException("Depo bulunamadı."));
            assetService.transferAsset(id, to, notes, getUser(username));
            return ResponseEntity.ok("Transfer tamamlandı.");
        } catch (SecurityException e) { return ResponseEntity.status(403).body(e.getMessage()); }
          catch (Exception e)          { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping("/{id}/maintenance/start")
    public ResponseEntity<?> startMaintenance(@AuthenticationPrincipal String username,
                                              @PathVariable int id,
                                              @RequestBody Map<String, String> body) {
        try {
            assetService.startMaintenance(id, body.get("description"), body.getOrDefault("notes", ""), getUser(username));
            return ResponseEntity.ok("Demirbaş bakıma alındı.");
        } catch (SecurityException e) { return ResponseEntity.status(403).body(e.getMessage()); }
          catch (Exception e)          { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping("/{id}/maintenance/end")
    public ResponseEntity<?> endMaintenance(@AuthenticationPrincipal String username,
                                            @PathVariable int id) {
        try {
            assetService.endMaintenance(id, getUser(username));
            return ResponseEntity.ok("Bakım tamamlandı.");
        } catch (SecurityException e) { return ResponseEntity.status(403).body(e.getMessage()); }
          catch (Exception e)          { return ResponseEntity.badRequest().body(e.getMessage()); }
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
    public List<Object[]> warehouseSummary() { return assetService.getWarehouseStatusSummary(); }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
    }
}
