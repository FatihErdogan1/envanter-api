package org.example.inventoryapi.service;

import org.example.inventoryapi.model.entity.User;
import org.example.inventoryapi.model.entity.Warehouse;
import org.example.inventoryapi.model.enums.AssetStatus;
import org.example.inventoryapi.model.enums.Role;
import org.example.inventoryapi.repository.AssetAssignmentRepository;
import org.example.inventoryapi.repository.AssetRepository;
import org.example.inventoryapi.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Testleri")
class UserServiceTest {

    @Mock UserRepository            userRepository;
    @Mock AssetAssignmentRepository assignmentRepository;
    @Mock AssetRepository           assetRepository;
    @Mock PasswordEncoder           passwordEncoder;
    @Mock MailService               mailService;
    @InjectMocks UserService service;

    // ── registerUser ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Geçerli kullanıcı kaydedilince pasif olarak oluşturulmalı")
    void registerUser_gecerliVeri_pasifOlarakKaydedilir() {
        User user = user("ali", "ali@test.com", "Sifre1234!");
        when(userRepository.existsByEmail("ali@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("ali")).thenReturn(false);
        when(passwordEncoder.encode("Sifre1234!")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = service.registerUser(user);

        assertThat(result.isActive()).isFalse();
        assertThat(result.getPasswordHash()).isEqualTo("hashed");
    }

    @Test
    @DisplayName("Kayıtlı e-posta ile kayıt yapılınca hata fırlatılmalı")
    void registerUser_mukerrerEmail_hataFirlatir() {
        User user = user("ali", "ali@test.com", "Sifre1234!");
        when(userRepository.existsByEmail("ali@test.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registerUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("e-posta");
    }

    @Test
    @DisplayName("Kayıtlı kullanıcı adıyla kayıt yapılınca hata fırlatılmalı")
    void registerUser_mukerrerKullaniciAdi_hataFirlatir() {
        User user = user("ali", "ali@test.com", "Sifre1234!");
        when(userRepository.existsByEmail("ali@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("ali")).thenReturn(true);

        assertThatThrownBy(() -> service.registerUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kullanıcı adı");
    }

    @Test
    @DisplayName("Geçersiz e-posta formatıyla kayıt yapılınca hata fırlatılmalı")
    void registerUser_gecersizEmail_hataFirlatir() {
        User user = user("ali", "gecersiz", "Sifre1234!");

        assertThatThrownBy(() -> service.registerUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("e-posta");
    }

    @Test
    @DisplayName("Zayıf şifreyle kayıt yapılınca hata fırlatılmalı")
    void registerUser_zayifSifre_hataFirlatir() {
        User user = user("ali", "ali@test.com", "12345");
        when(userRepository.existsByEmail("ali@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("ali")).thenReturn(false);

        assertThatThrownBy(() -> service.registerUser(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Şifre");
    }

    // ── createUserByAdmin ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Admin tarafından oluşturulan kullanıcı aktif olarak kaydedilmeli")
    void createUserByAdmin_gecerliVeri_aktifOlarakKaydedilir() {
        User user = user("veli", "veli@test.com", "Sifre1234!");
        user.setActive(true);
        when(userRepository.existsByEmail("veli@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("veli")).thenReturn(false);
        when(passwordEncoder.encode("Sifre1234!")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = service.createUserByAdmin(user);

        assertThat(result.getPasswordHash()).isEqualTo("hashed");
    }

    // ── updateUserStatus ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Kullanıcı pasife alınınca aktif zimmetleri kapatılmalı ve varlıklar serbest bırakılmalı")
    void updateUserStatus_pasifEAliniyor_zimmleteriKapatirVarlikSerbest() {
        User user = userWithId(5, Role.STAFF);
        user.setActive(true);
        var asset = new org.example.inventoryapi.model.entity.Asset();
        asset.setStatus(AssetStatus.IN_USE);
        when(userRepository.findById(5)).thenReturn(Optional.of(user));
        when(assignmentRepository.findActiveAssetIdsByUserId(5)).thenReturn(List.of(10));
        when(assetRepository.findById(10)).thenReturn(Optional.of(asset));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateUserStatus(5, false, "STAFF");

        assertThat(asset.getStatus()).isEqualTo(AssetStatus.AVAILABLE);
        verify(assignmentRepository).closeAllActiveByUserId(5);
        verify(assetRepository).save(asset);
    }

    @Test
    @DisplayName("Kullanıcı aktife alınınca zimmet işlemi yapılmamalı")
    void updateUserStatus_aktifEAliniyor_zimmetIslemiyapilmaz() {
        User user = userWithId(5, Role.STAFF);
        user.setActive(false);
        when(userRepository.findById(5)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateUserStatus(5, true, "MANAGER");

        verify(assignmentRepository, never()).closeAllActiveByUserId(anyInt());
        assertThat(user.isActive()).isTrue();
        assertThat(user.getRole()).isEqualTo(Role.MANAGER);
    }

    // ── deleteUser ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Admin rolündeki kullanıcı silinemez")
    void deleteUser_adminRolu_hataFirlatir() {
        User admin = userWithId(1, Role.ADMIN);
        when(userRepository.findById(1)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.deleteUser(1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Admin");
    }

    @Test
    @DisplayName("Aktif zimmeti olan kullanıcı silinemez")
    void deleteUser_aktifZimmetVar_hataFirlatir() {
        User user = userWithId(2, Role.STAFF);
        when(userRepository.findById(2)).thenReturn(Optional.of(user));
        when(assignmentRepository.countByUserIdAndReturnDateIsNull(2)).thenReturn(1);

        assertThatThrownBy(() -> service.deleteUser(2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("zimmet");
    }

    @Test
    @DisplayName("Zimmetsiz STAFF kullanıcısı başarıyla silinmeli")
    void deleteUser_zimmetsizStaff_silmeBasarili() {
        User user = userWithId(2, Role.STAFF);
        when(userRepository.findById(2)).thenReturn(Optional.of(user));
        when(assignmentRepository.countByUserIdAndReturnDateIsNull(2)).thenReturn(0);

        assertThatNoException().isThrownBy(() -> service.deleteUser(2));
        verify(userRepository).deleteById(2);
    }

    // ── resetPassword ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Admin şifresi sıfırlanamaz")
    void resetPassword_adminRolu_hataFirlatir() {
        User admin = userWithId(1, Role.ADMIN);
        when(userRepository.findById(1)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.resetPassword(1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Admin");
    }

    @Test
    @DisplayName("Staff şifresi sıfırlanınca geçici şifre oluşturulmalı ve mail gönderilmeli")
    void resetPassword_staffRolu_geciciSifreOlusturulur() {
        User user = userWithId(2, Role.STAFF);
        user.setEmail("staff@test.com");
        when(userRepository.findById(2)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(user);

        String tempPassword = service.resetPassword(2);

        assertThat(tempPassword).isNotBlank();
        assertThat(user.isForcePasswordChange()).isTrue();
        assertThat(user.isActive()).isTrue();
        verify(mailService).sendPasswordReset(eq("staff@test.com"), anyString());
    }

    // ── changePassword ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Yanlış mevcut şifre girilince hata fırlatılmalı")
    void changePassword_yanlisMevcutSifre_hataFirlatir() {
        User user = userWithId(3, Role.STAFF);
        user.setPasswordHash("correctHash");
        when(userRepository.findById(3)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("yanlis", "correctHash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(3, "yanlis", "YeniSifre1!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mevcut şifre");
    }

    @Test
    @DisplayName("Zayıf yeni şifre girilince hata fırlatılmalı")
    void changePassword_zayifYeniSifre_hataFirlatir() {
        assertThatThrownBy(() -> service.changePassword(3, null, "kisa"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Şifre");
    }

    @Test
    @DisplayName("Geçerli şifre değişikliğinde forcePasswordChange temizlenmeli")
    void changePassword_gecerliVeri_forcePasswordChangeTemizlenir() {
        User user = userWithId(3, Role.STAFF);
        user.setPasswordHash("oldHash");
        user.setForcePasswordChange(true);
        when(userRepository.findById(3)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("eskiSifre", "oldHash")).thenReturn(true);
        when(passwordEncoder.encode("YeniSifre1!")).thenReturn("newHash");
        when(userRepository.save(any())).thenReturn(user);

        service.changePassword(3, "eskiSifre", "YeniSifre1!");

        assertThat(user.isForcePasswordChange()).isFalse();
        assertThat(user.getPasswordHash()).isEqualTo("newHash");
    }

    // ── getAssignableUsers ────────────────────────────────────────────────────

    @Test
    @DisplayName("Admin tüm aktif non-admin kullanıcıları görebilmeli")
    void getAssignableUsers_adminRequester_tumAktifNonAdminleriDondurir() {
        User admin    = userWithId(1, Role.ADMIN);
        User staff1   = userWithId(2, Role.STAFF); staff1.setActive(true);
        User staff2   = userWithId(3, Role.STAFF); staff2.setActive(false);
        User adminUser = userWithId(4, Role.ADMIN); adminUser.setActive(true);
        when(userRepository.findAll()).thenReturn(List.of(admin, staff1, staff2, adminUser));

        List<User> result = service.getAssignableUsers(admin);

        assertThat(result).containsExactly(staff1);
    }

    @Test
    @DisplayName("Manager yalnızca kendi deposundaki kullanıcıları görebilmeli")
    void getAssignableUsers_managerRequester_sadecKendiDeposundakiKullanicilari() {
        Warehouse depot1 = warehouseWithId(1);
        Warehouse depot2 = warehouseWithId(2);

        User manager = userWithId(10, Role.MANAGER); manager.setWarehouse(depot1);
        User staff1  = userWithId(11, Role.STAFF);  staff1.setActive(true); staff1.setWarehouse(depot1);
        User staff2  = userWithId(12, Role.STAFF);  staff2.setActive(true); staff2.setWarehouse(depot2);
        when(userRepository.findAll()).thenReturn(List.of(manager, staff1, staff2));

        List<User> result = service.getAssignableUsers(manager);

        assertThat(result).containsExactly(staff1);
    }

    // ── yardımcı ──────────────────────────────────────────────────────────────

    private User user(String username, String email, String password) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(password);
        return u;
    }

    private User userWithId(int id, Role role) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        u.setEmail("user" + id + "@test.com");
        return u;
    }

    private Warehouse warehouseWithId(int id) {
        Warehouse w = new Warehouse();
        w.setId(id);
        return w;
    }
}
