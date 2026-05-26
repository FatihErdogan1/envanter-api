package org.example.inventoryapi.service;

import org.example.inventoryapi.exception.DeletionBlockedException;
import org.example.inventoryapi.model.entity.Category;
import org.example.inventoryapi.repository.CategoryRepository;
import org.example.inventoryapi.repository.ProductRepository;
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
@DisplayName("CategoryService Testleri")
class CategoryServiceTest {

    @Mock CategoryRepository categoryRepository;
    @Mock ProductRepository  productRepository;
    @InjectMocks CategoryService service;

    // ── addCategory ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Geçerli kategori eklenince kaydedilmeli")
    void addCategory_gecerliIsim_kategorikKaydedilir() {
        Category cat = category("Elektronik");
        when(categoryRepository.existsByNameIgnoreCase("Elektronik")).thenReturn(false);
        when(categoryRepository.save(cat)).thenReturn(cat);

        Category result = service.addCategory(cat);

        assertThat(result).isEqualTo(cat);
        verify(categoryRepository).save(cat);
    }

    @Test
    @DisplayName("Boş kategori adıyla ekleme yapılınca hata fırlatılmalı")
    void addCategory_bosIsim_hataFirlatir() {
        assertThatThrownBy(() -> service.addCategory(category("   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("boş");
    }

    @Test
    @DisplayName("Null kategori adıyla ekleme yapılınca hata fırlatılmalı")
    void addCategory_nullIsim_hataFirlatir() {
        Category cat = new Category();
        assertThatThrownBy(() -> service.addCategory(cat))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Aynı isimde kategori varsa hata fırlatılmalı")
    void addCategory_mukerrerIsim_hataFirlatir() {
        Category cat = category("Elektronik");
        when(categoryRepository.existsByNameIgnoreCase("Elektronik")).thenReturn(true);

        assertThatThrownBy(() -> service.addCategory(cat))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kayıtlıdır");
    }

    // ── updateCategory ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Mevcut kategori güncellenince alanlar değişmeli")
    void updateCategory_mevcutId_guncellemeyiKaydeder() {
        Category existing = category("Eski Ad");
        Category updated  = category("Yeni Ad");
        updated.setDescription("Açıklama");
        when(categoryRepository.findById(1)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(existing)).thenReturn(existing);

        Category result = service.updateCategory(1, updated);

        assertThat(result.getName()).isEqualTo("Yeni Ad");
        assertThat(result.getDescription()).isEqualTo("Açıklama");
    }

    @Test
    @DisplayName("Var olmayan kategori güncellenmeye çalışılınca hata fırlatılmalı")
    void updateCategory_bulunamayanId_hataFirlatir() {
        when(categoryRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCategory(99, new Category()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bulunamadı");
    }

    // ── deleteCategory ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Ürünü olmayan kategori başarıyla silinmeli")
    void deleteCategory_urunYok_silmeBasarili() {
        when(productRepository.countByCategoryId(1)).thenReturn(0);

        assertThatNoException().isThrownBy(() -> service.deleteCategory(1));
        verify(categoryRepository).deleteById(1);
    }

    @Test
    @DisplayName("Ürünü olan kategori silinmeye çalışılınca DeletionBlockedException fırlatılmalı")
    void deleteCategory_urunVar_engellenir() {
        when(productRepository.countByCategoryId(1)).thenReturn(3);

        assertThatThrownBy(() -> service.deleteCategory(1))
                .isInstanceOf(DeletionBlockedException.class)
                .hasMessageContaining("ürün");
    }

    // ── yardımcı ──────────────────────────────────────────────────────────────

    private Category category(String name) {
        Category c = new Category();
        c.setName(name);
        return c;
    }
}
