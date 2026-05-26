package org.example.inventoryapi.service;

import org.example.inventoryapi.exception.DeletionBlockedException;
import org.example.inventoryapi.model.entity.Warehouse;
import org.example.inventoryapi.repository.AssetRepository;
import org.example.inventoryapi.repository.InventoryTransactionRepository;
import org.example.inventoryapi.repository.ProductRepository;
import org.example.inventoryapi.repository.WarehouseRepository;
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
@DisplayName("WarehouseService Testleri")
class WarehouseServiceTest {

    @Mock WarehouseRepository            warehouseRepository;
    @Mock AssetRepository                assetRepository;
    @Mock ProductRepository              productRepository;
    @Mock InventoryTransactionRepository transactionRepository;
    @InjectMocks WarehouseService service;

    // ── addWarehouse ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Geçerli depo eklenince kaydedilmeli")
    void addWarehouse_gecerliIsim_kaydedilir() {
        Warehouse w = warehouse("Merkez Depo");
        when(warehouseRepository.existsByNameIgnoreCase("Merkez Depo")).thenReturn(false);
        when(warehouseRepository.save(w)).thenReturn(w);

        Warehouse result = service.addWarehouse(w);

        assertThat(result).isEqualTo(w);
        verify(warehouseRepository).save(w);
    }

    @Test
    @DisplayName("Boş depo adıyla ekleme yapılınca hata fırlatılmalı")
    void addWarehouse_bosIsim_hataFirlatir() {
        assertThatThrownBy(() -> service.addWarehouse(warehouse("")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("boş");
    }

    @Test
    @DisplayName("Aynı isimde depo varsa hata fırlatılmalı")
    void addWarehouse_mukerrerIsim_hataFirlatir() {
        Warehouse w = warehouse("Merkez Depo");
        when(warehouseRepository.existsByNameIgnoreCase("Merkez Depo")).thenReturn(true);

        assertThatThrownBy(() -> service.addWarehouse(w))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kayıtlıdır");
    }

    // ── updateWarehouse ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Mevcut depo güncellenince alanlar değişmeli")
    void updateWarehouse_mevcutId_guncellemeyiKaydeder() {
        Warehouse existing = warehouse("Eski Ad");
        Warehouse updated  = warehouse("Yeni Ad");
        updated.setLocationAddress("İstanbul");
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(existing));
        when(warehouseRepository.save(existing)).thenReturn(existing);

        Warehouse result = service.updateWarehouse(1, updated);

        assertThat(result.getName()).isEqualTo("Yeni Ad");
        assertThat(result.getLocationAddress()).isEqualTo("İstanbul");
    }

    @Test
    @DisplayName("Var olmayan depo güncellenmeye çalışılınca hata fırlatılmalı")
    void updateWarehouse_bulunamayanId_hataFirlatir() {
        when(warehouseRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateWarehouse(99, new Warehouse()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bulunamadı");
    }

    // ── deleteWarehouse ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Boş depo başarıyla silinmeli")
    void deleteWarehouse_tumKontrollerGeciyor_silmeBasarili() {
        when(transactionRepository.getTotalStockInWarehouse(1)).thenReturn(0L);
        when(assetRepository.countByWarehouseId(1)).thenReturn(0);
        when(productRepository.countByWarehouseId(1)).thenReturn(0);

        assertThatNoException().isThrownBy(() -> service.deleteWarehouse(1));
        verify(warehouseRepository).deleteById(1);
    }

    @Test
    @DisplayName("Stok kaydı olan depo silinmeye çalışılınca DeletionBlockedException fırlatılmalı")
    void deleteWarehouse_stokVar_engellenir() {
        when(transactionRepository.getTotalStockInWarehouse(1)).thenReturn(50L);

        assertThatThrownBy(() -> service.deleteWarehouse(1))
                .isInstanceOf(DeletionBlockedException.class)
                .hasMessageContaining("stok");
    }

    @Test
    @DisplayName("Demirbaşı olan depo silinmeye çalışılınca DeletionBlockedException fırlatılmalı")
    void deleteWarehouse_demirbaskiVar_engellenir() {
        when(transactionRepository.getTotalStockInWarehouse(1)).thenReturn(0L);
        when(assetRepository.countByWarehouseId(1)).thenReturn(2);

        assertThatThrownBy(() -> service.deleteWarehouse(1))
                .isInstanceOf(DeletionBlockedException.class)
                .hasMessageContaining("demirbaş");
    }

    @Test
    @DisplayName("Ürünü olan depo silinmeye çalışılınca DeletionBlockedException fırlatılmalı")
    void deleteWarehouse_urunVar_engellenir() {
        when(transactionRepository.getTotalStockInWarehouse(1)).thenReturn(0L);
        when(assetRepository.countByWarehouseId(1)).thenReturn(0);
        when(productRepository.countByWarehouseId(1)).thenReturn(5);

        assertThatThrownBy(() -> service.deleteWarehouse(1))
                .isInstanceOf(DeletionBlockedException.class)
                .hasMessageContaining("ürün");
    }

    // ── yardımcı ──────────────────────────────────────────────────────────────

    private Warehouse warehouse(String name) {
        Warehouse w = new Warehouse();
        w.setName(name);
        return w;
    }
}
