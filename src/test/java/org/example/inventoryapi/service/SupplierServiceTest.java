package org.example.inventoryapi.service;

import org.example.inventoryapi.exception.DeletionBlockedException;
import org.example.inventoryapi.model.entity.Supplier;
import org.example.inventoryapi.repository.AssetRepository;
import org.example.inventoryapi.repository.ProductRepository;
import org.example.inventoryapi.repository.SupplierRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupplierService Testleri")
class SupplierServiceTest {

    @Mock SupplierRepository supplierRepository;
    @Mock AssetRepository    assetRepository;
    @Mock ProductRepository  productRepository;
    @InjectMocks SupplierService service;

    // ── addSupplier ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Geçerli tedarikçi eklenince kaydedilmeli")
    void addSupplier_gecerliVeri_kaydedilir() {
        Supplier s = supplier("TechParts", "info@techparts.com", "+90 212 555 0101");
        when(supplierRepository.existsByNameIgnoreCase("TechParts")).thenReturn(false);
        when(supplierRepository.existsByContactEmailIgnoreCase("info@techparts.com")).thenReturn(false);
        when(supplierRepository.existsByPhone("+90 212 555 0101")).thenReturn(false);
        when(supplierRepository.save(s)).thenReturn(s);

        Supplier result = service.addSupplier(s);

        assertThat(result).isEqualTo(s);
        verify(supplierRepository).save(s);
    }

    @Test
    @DisplayName("Boş tedarikçi adıyla ekleme yapılınca hata fırlatılmalı")
    void addSupplier_bosIsim_hataFirlatir() {
        assertThatThrownBy(() -> service.addSupplier(supplier("", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("boş");
    }

    @Test
    @DisplayName("Mükerrer tedarikçi adıyla ekleme yapılınca hata fırlatılmalı")
    void addSupplier_mukerrerIsim_hataFirlatir() {
        Supplier s = supplier("TechParts", null, null);
        when(supplierRepository.existsByNameIgnoreCase("TechParts")).thenReturn(true);

        assertThatThrownBy(() -> service.addSupplier(s))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kayıtlıdır");
    }

    @Test
    @DisplayName("Geçersiz e-posta formatıyla ekleme yapılınca hata fırlatılmalı")
    void addSupplier_gecersizEmail_hataFirlatir() {
        Supplier s = supplier("TechParts", "gecersiz-email", null);
        when(supplierRepository.existsByNameIgnoreCase("TechParts")).thenReturn(false);

        assertThatThrownBy(() -> service.addSupplier(s))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("e-posta");
    }

    @Test
    @DisplayName("Mükerrer e-posta adresiyle ekleme yapılınca hata fırlatılmalı")
    void addSupplier_mukerrerEmail_hataFirlatir() {
        Supplier s = supplier("TechParts", "info@techparts.com", null);
        when(supplierRepository.existsByNameIgnoreCase("TechParts")).thenReturn(false);
        when(supplierRepository.existsByContactEmailIgnoreCase("info@techparts.com")).thenReturn(true);

        assertThatThrownBy(() -> service.addSupplier(s))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("e-posta");
    }

    @Test
    @DisplayName("20 karakterden uzun telefon numarasıyla ekleme yapılınca hata fırlatılmalı")
    void addSupplier_cokUzunTelefon_hataFirlatir() {
        Supplier s = supplier("TechParts", null, "1234567890123456789012345");
        when(supplierRepository.existsByNameIgnoreCase("TechParts")).thenReturn(false);

        assertThatThrownBy(() -> service.addSupplier(s))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("20");
    }

    @Test
    @DisplayName("Mükerrer telefon numarasıyla ekleme yapılınca hata fırlatılmalı")
    void addSupplier_mukerrerTelefon_hataFirlatir() {
        Supplier s = supplier("TechParts", null, "+90 212 555 0101");
        when(supplierRepository.existsByNameIgnoreCase("TechParts")).thenReturn(false);
        when(supplierRepository.existsByPhone("+90 212 555 0101")).thenReturn(true);

        assertThatThrownBy(() -> service.addSupplier(s))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("telefon");
    }

    // ── updateSupplier ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Mevcut tedarikçi güncellenince alanlar değişmeli")
    void updateSupplier_mevcutId_guncellemeyiKaydeder() {
        Supplier existing = supplier("Eski Ad", "eski@mail.com", "111");
        Supplier updated  = supplier("Yeni Ad", "yeni@mail.com", "222");
        updated.setAddress("İstanbul");
        when(supplierRepository.findById(1)).thenReturn(Optional.of(existing));
        when(supplierRepository.existsByContactEmailIgnoreCaseAndIdNot("yeni@mail.com", 1)).thenReturn(false);
        when(supplierRepository.existsByPhoneAndIdNot("222", 1)).thenReturn(false);
        when(supplierRepository.save(existing)).thenReturn(existing);

        Supplier result = service.updateSupplier(1, updated);

        assertThat(result.getName()).isEqualTo("Yeni Ad");
        assertThat(result.getContactEmail()).isEqualTo("yeni@mail.com");
        assertThat(result.getAddress()).isEqualTo("İstanbul");
    }

    @Test
    @DisplayName("Var olmayan tedarikçi güncellenmeye çalışılınca hata fırlatılmalı")
    void updateSupplier_bulunamayanId_hataFirlatir() {
        when(supplierRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSupplier(99, new Supplier()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bulunamadı");
    }

    @Test
    @DisplayName("Güncelleme sırasında başka tedarikçinin telefonu girilince hata fırlatılmalı")
    void updateSupplier_mukerrerTelefon_hataFirlatir() {
        Supplier existing = supplier("TechParts", null, "111");
        Supplier updated  = supplier("TechParts", null, "999");
        when(supplierRepository.findById(1)).thenReturn(Optional.of(existing));
        when(supplierRepository.existsByPhoneAndIdNot("999", 1)).thenReturn(true);

        assertThatThrownBy(() -> service.updateSupplier(1, updated))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("telefon");
    }

    // ── deleteSupplier ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Ürünü ve demirbaşı olmayan tedarikçi başarıyla silinmeli")
    void deleteSupplier_baglantisiz_silmeBasarili() {
        when(productRepository.countBySupplierId(1)).thenReturn(0);
        when(assetRepository.countBySupplierId(1)).thenReturn(0);

        assertThatNoException().isThrownBy(() -> service.deleteSupplier(1));
        verify(supplierRepository).deleteById(1);
    }

    @Test
    @DisplayName("Ürünü olan tedarikçi silinmeye çalışılınca DeletionBlockedException fırlatılmalı")
    void deleteSupplier_urunVar_engellenir() {
        when(productRepository.countBySupplierId(1)).thenReturn(2);

        assertThatThrownBy(() -> service.deleteSupplier(1))
                .isInstanceOf(DeletionBlockedException.class)
                .hasMessageContaining("ürün");
    }

    @Test
    @DisplayName("Demirbaşı olan tedarikçi silinmeye çalışılınca DeletionBlockedException fırlatılmalı")
    void deleteSupplier_demirbaskiVar_engellenir() {
        when(productRepository.countBySupplierId(1)).thenReturn(0);
        when(assetRepository.countBySupplierId(1)).thenReturn(3);

        assertThatThrownBy(() -> service.deleteSupplier(1))
                .isInstanceOf(DeletionBlockedException.class)
                .hasMessageContaining("demirbaş");
    }

    // ── yardımcı ──────────────────────────────────────────────────────────────

    private Supplier supplier(String name, String email, String phone) {
        Supplier s = new Supplier();
        s.setName(name);
        s.setContactEmail(email);
        s.setPhone(phone);
        return s;
    }
}
