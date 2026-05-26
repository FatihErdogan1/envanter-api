package org.example.inventoryapi.service;

import org.example.inventoryapi.model.entity.User;
import org.example.inventoryapi.model.enums.AssetStatus;
import org.example.inventoryapi.model.enums.Role;
import org.example.inventoryapi.repository.AssetAssignmentRepository;
import org.example.inventoryapi.repository.AssetRepository;
import org.example.inventoryapi.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*(),.?\":{}|<>]).{8,}$");

    private final UserRepository             userRepository;
    private final AssetAssignmentRepository  assignmentRepository;
    private final AssetRepository            assetRepository;
    private final PasswordEncoder            passwordEncoder;
    private final MailService                mailService;

    public UserService(UserRepository userRepository,
                       AssetAssignmentRepository assignmentRepository,
                       AssetRepository assetRepository,
                       PasswordEncoder passwordEncoder,
                       MailService mailService) {
        this.userRepository      = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.assetRepository     = assetRepository;
        this.passwordEncoder     = passwordEncoder;
        this.mailService         = mailService;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getAssignableUsers(User requester) {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() != Role.ADMIN)
                .filter(User::isActive)
                .filter(user -> requester.getRole() == Role.ADMIN
                        || requester.getWarehouse() == null
                        || (requester.getWarehouse() != null
                            && user.getWarehouse() != null
                            && requester.getWarehouse().getId() == user.getWarehouse().getId()))
                .toList();
    }

    public User registerUser(User user) {
        validateEmail(user.getEmail());
        if (userRepository.existsByEmail(user.getEmail()))
            throw new IllegalArgumentException("Bu e-posta adresi zaten kayıtlıdır.");
        if (userRepository.existsByUsername(user.getUsername()))
            throw new IllegalArgumentException("Bu kullanıcı adı zaten kullanılıyor.");
        validatePassword(user.getPasswordHash());
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setActive(false);
        return userRepository.save(user);
    }

    @Transactional
    public User updateUserStatus(int userId, boolean isActive, String roleStr) {
        User user = getOrThrow(userId);
        if (!isActive && user.isActive()) {
            List<Integer> assetIds = assignmentRepository.findActiveAssetIdsByUserId(userId);
            assignmentRepository.closeAllActiveByUserId(userId);
            assetIds.forEach(id -> assetRepository.findById(id).ifPresent(a -> {
                a.setStatus(AssetStatus.AVAILABLE);
                assetRepository.save(a);
            }));
        }
        user.setActive(isActive);
        user.setRole(Role.valueOf(roleStr));
        return userRepository.save(user);
    }

    public User updateUserInfo(int userId, String newUsername, String newEmail) {
        validateEmail(newEmail);
        if (userRepository.existsByEmailAndIdNot(newEmail, userId))
            throw new IllegalArgumentException("Bu e-posta adresi başka bir kullanıcıya kayıtlıdır.");
        if (userRepository.existsByUsernameAndIdNot(newUsername, userId))
            throw new IllegalArgumentException("Bu kullanıcı adı zaten kullanılıyor.");
        User user = getOrThrow(userId);
        user.setUsername(newUsername);
        user.setEmail(newEmail);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(int userId) {
        User user = getOrThrow(userId);
        if (user.getRole() == Role.ADMIN)
            throw new IllegalStateException("Admin rolündeki kullanıcılar silinemez.");
        int activeCount = assignmentRepository.countByUserIdAndReturnDateIsNull(userId);
        if (activeCount > 0)
            throw new IllegalStateException(
                "Bu kullanıcıda " + activeCount + " adet aktif zimmet var. Silmeden önce zimmetleri iade ediniz.");
        userRepository.deleteById(userId);
    }

    public String resetPassword(int userId) {
        User user = getOrThrow(userId);
        if (user.getRole() == Role.ADMIN)
            throw new IllegalStateException("Admin şifresi bu yolla sıfırlanamaz.");
        String tempPassword = generateTempPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setForcePasswordChange(true);
        user.setActive(true);
        userRepository.save(user);
        mailService.sendPasswordReset(user.getEmail(), tempPassword);
        return tempPassword;
    }

    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getRole() == Role.ADMIN) return;
            String tempPassword = generateTempPassword();
            // Mail önce gönderilir; başarısız olursa şifre güncellenmez
            mailService.sendPasswordReset(user.getEmail(), tempPassword);
            user.setPasswordHash(passwordEncoder.encode(tempPassword));
            user.setForcePasswordChange(true);
            userRepository.save(user);
        });
    }

    public User getById(int userId) {
        return getOrThrow(userId);
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı: " + username));
    }

    public User createUserByAdmin(User user) {
        validateEmail(user.getEmail());
        if (userRepository.existsByEmail(user.getEmail()))
            throw new IllegalArgumentException("Bu e-posta adresi zaten kayıtlıdır.");
        if (userRepository.existsByUsername(user.getUsername()))
            throw new IllegalArgumentException("Bu kullanıcı adı zaten kullanılıyor.");
        validatePassword(user.getPasswordHash());
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        return userRepository.save(user);
    }

    public User updateUserWarehouse(int userId, org.example.inventoryapi.model.entity.Warehouse warehouse) {
        User user = getOrThrow(userId);
        user.setWarehouse(warehouse);
        return userRepository.save(user);
    }

    public User updateUserSupplier(int userId, org.example.inventoryapi.model.entity.Supplier supplier) {
        User user = getOrThrow(userId);
        user.setSupplier(supplier);
        return userRepository.save(user);
    }

    public void changePassword(int userId, String currentPassword, String newPassword) {
        validatePassword(newPassword);
        User user = getOrThrow(userId);
        if (currentPassword != null && !passwordEncoder.matches(currentPassword, user.getPasswordHash()))
            throw new IllegalArgumentException("Mevcut şifre hatalı.");
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setForcePasswordChange(false);
        userRepository.save(user);
    }

    private User getOrThrow(int userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı: " + userId));
    }

    private void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches())
            throw new IllegalArgumentException("Geçerli bir e-posta adresi giriniz (örn: kullanici@alan.com).");
    }

    private void validatePassword(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches())
            throw new IllegalArgumentException(
                "Şifre en az 8 karakter olmalı; büyük/küçük harf, rakam ve özel karakter içermelidir.");
    }

    private String generateTempPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghjkmnpqrstuvwxyz";
        String digits = "23456789";
        String special = "!@#$%&*";
        String all = upper + lower + digits + special;
        SecureRandom rng = new SecureRandom();
        char[] pw = {
            upper.charAt(rng.nextInt(upper.length())),
            upper.charAt(rng.nextInt(upper.length())),
            lower.charAt(rng.nextInt(lower.length())),
            lower.charAt(rng.nextInt(lower.length())),
            digits.charAt(rng.nextInt(digits.length())),
            digits.charAt(rng.nextInt(digits.length())),
            special.charAt(rng.nextInt(special.length())),
            all.charAt(rng.nextInt(all.length())),
            all.charAt(rng.nextInt(all.length())),
            all.charAt(rng.nextInt(all.length()))
        };
        for (int i = pw.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            char tmp = pw[i]; pw[i] = pw[j]; pw[j] = tmp;
        }
        return new String(pw);
    }
}
