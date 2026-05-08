package org.example.inventoryapi.controller;

import org.example.inventoryapi.dto.AuthResponse;
import org.example.inventoryapi.dto.ChangePasswordRequest;
import org.example.inventoryapi.dto.ForgotPasswordRequest;
import org.example.inventoryapi.dto.LoginRequest;
import org.example.inventoryapi.dto.RegisterRequest;
import org.example.inventoryapi.model.entity.User;
import org.example.inventoryapi.model.enums.Role;
import org.example.inventoryapi.repository.UserRepository;
import org.example.inventoryapi.security.JwtUtil;
import org.example.inventoryapi.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil         jwtUtil;
    private final UserService     userService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil, UserService userService) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil         = jwtUtil;
        this.userService     = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
            return ResponseEntity.status(401).body("Hatalı kullanıcı adı veya şifre.");

        if (!user.isActive())
            return ResponseEntity.status(403).body("Hesabınız pasif. Yöneticinizle iletişime geçin.");

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        Integer warehouseId   = user.getWarehouse() != null ? user.getWarehouse().getId() : null;
        String  warehouseName = user.getWarehouse() != null ? user.getWarehouse().getName() : null;
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(),
                user.getRole().name(), user.isForcePasswordChange(), warehouseId, warehouseName));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(request.getPassword());
        user.setRole(Role.STAFF);
        try {
            userService.registerUser(user);
            return ResponseEntity.ok("Kayıt başarılı. Hesabınız yönetici onayı bekleniyor.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request.getEmail());
        return ResponseEntity.ok("E-posta adresiniz kayıtlıysa geçici şifre gönderildi.");
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal String username,
                                             @RequestBody ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Kullanıcı bulunamadı."));
        try {
            userService.changePassword(user.getId(), request.getNewPassword());
            return ResponseEntity.ok("Şifre başarıyla güncellendi.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
