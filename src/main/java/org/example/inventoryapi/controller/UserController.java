package org.example.inventoryapi.controller;

import org.example.inventoryapi.model.entity.Supplier;
import org.example.inventoryapi.model.entity.User;
import org.example.inventoryapi.model.entity.Warehouse;
import org.example.inventoryapi.model.enums.Role;
import org.example.inventoryapi.repository.SupplierRepository;
import org.example.inventoryapi.repository.WarehouseRepository;
import org.example.inventoryapi.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService         userService;
    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository  supplierRepository;

    public UserController(UserService userService, WarehouseRepository warehouseRepository,
                          SupplierRepository supplierRepository) {
        this.userService         = userService;
        this.warehouseRepository = warehouseRepository;
        this.supplierRepository  = supplierRepository;
    }

    @GetMapping
    public List<User> getAllUsers(@AuthenticationPrincipal String username) {
        return userService.getAllUsers().stream()
                .filter(u -> !u.getUsername().equals(username))
                .toList();
    }

    @GetMapping("/assignable")
    public List<User> getAssignableUsers(@AuthenticationPrincipal String username) {
        User requester = userService.getByUsername(username);
        return userService.getAssignableUsers(requester);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        try {
            String roleStr = (String) body.getOrDefault("role", "STAFF");
            if ("ADMIN".equals(roleStr))
                return ResponseEntity.badRequest().body("Admin rolünde kullanıcı oluşturulamaz.");

            User user = new User();
            user.setUsername((String) body.get("username"));
            user.setEmail((String) body.get("email"));
            user.setPasswordHash((String) body.get("password"));
            user.setRole(Role.valueOf(roleStr));
            user.setActive(true);

            Object whIdObj = body.get("warehouseId");
            if (whIdObj != null && !(whIdObj instanceof String s && s.isBlank())) {
                int warehouseId = whIdObj instanceof Integer i ? i : Integer.parseInt(whIdObj.toString());
                Warehouse warehouse = warehouseRepository.findById(warehouseId)
                        .orElseThrow(() -> new IllegalArgumentException("Depo bulunamadı."));
                user.setWarehouse(warehouse);
            }

            Object spIdObj = body.get("supplierId");
            if (spIdObj != null && !(spIdObj instanceof String s && s.isBlank())) {
                int supplierId = spIdObj instanceof Integer i ? i : Integer.parseInt(spIdObj.toString());
                Supplier supplier = supplierRepository.findById(supplierId)
                        .orElseThrow(() -> new IllegalArgumentException("Tedarikçi bulunamadı."));
                user.setSupplier(supplier);
            }
            return ResponseEntity.ok(userService.createUserByAdmin(user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@AuthenticationPrincipal String requesterUsername,
                                    @PathVariable int id,
                                    @RequestBody Map<String, Object> body) {
        try {
            User target = userService.getById(id);
            if (target.getRole() == Role.ADMIN)
                return ResponseEntity.badRequest().body("Admin kullanıcıları düzenlenemez.");
            if (target.getUsername().equals(requesterUsername))
                return ResponseEntity.badRequest().body("Kendi hesabınızı bu sayfadan düzenleyemezsiniz.");

            String username  = (String) body.get("username");
            String email     = (String) body.get("email");
            boolean isActive = body.get("active") != null ? (boolean) body.get("active") : true;
            String roleStr   = (String) body.get("role");
            if ("ADMIN".equals(roleStr))
                return ResponseEntity.badRequest().body("Kullanıcı rolü Admin olarak değiştirilemez.");

            userService.updateUserInfo(id, username, email);
            userService.updateUserStatus(id, isActive, roleStr);
            if (body.containsKey("warehouseId")) {
                Integer warehouseId = body.get("warehouseId") != null ? (Integer) body.get("warehouseId") : null;
                Warehouse warehouse = warehouseId != null
                        ? warehouseRepository.findById(warehouseId)
                                .orElseThrow(() -> new IllegalArgumentException("Depo bulunamadı."))
                        : null;
                userService.updateUserWarehouse(id, warehouse);
            }
            if (body.containsKey("supplierId")) {
                Object spIdObj = body.get("supplierId");
                if (spIdObj != null) {
                    int supplierId = spIdObj instanceof Integer i ? i : Integer.parseInt(spIdObj.toString());
                    Supplier supplier = supplierRepository.findById(supplierId)
                            .orElseThrow(() -> new IllegalArgumentException("Tedarikçi bulunamadı."));
                    userService.updateUserSupplier(id, supplier);
                } else {
                    userService.updateUserSupplier(id, null);
                }
            }
            return ResponseEntity.ok(userService.getById(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal String requesterUsername,
                                        @PathVariable int id) {
        try {
            User target = userService.getById(id);
            if (target.getUsername().equals(requesterUsername))
                return ResponseEntity.badRequest().body("Kendi hesabınızı silemezsiniz.");
            userService.deleteUser(id);
            return ResponseEntity.ok("Kullanıcı silindi.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable int id) {
        try {
            String tempPassword = userService.resetPassword(id);
            return ResponseEntity.ok(Map.of("tempPassword", tempPassword));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
