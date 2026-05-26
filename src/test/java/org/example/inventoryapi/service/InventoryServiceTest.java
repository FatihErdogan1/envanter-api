package org.example.inventoryapi.service;

import org.example.inventoryapi.model.entity.*;
import org.example.inventoryapi.model.enums.Role;
import org.example.inventoryapi.repository.InventoryTransactionRepository;
import org.example.inventoryapi.repository.ProductRepository;
import org.example.inventoryapi.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Testleri")
class InventoryServiceTest {

    @Mock InventoryTransactionRepository transactionRepository;
    @Mock ProductRepository              productRepository;
    @Mock UserRepository                 userRepository;
    @Mock NotificationService            notificationService;
    @InjectMocks InventoryService service;

    // ── stockIn ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Geçerli stok girişi kaydedilmeli ve ürün stoğu artmalı")
    void stockIn_gecerliVeri_stokArtarVeKaydedilir() {
        InventoryTransaction tx = txIn(10, warehouseWithId(1), productWithId(1, 5));
        User admin = adminUser();
        Product product = productWithId(1, 5);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(transactionRepository.save(tx)).thenReturn(tx);
        when(userRepository.findByRole(any())).thenReturn(List.of());

        service.stockIn(tx, admin);

        assertThat(product.getQuantityInStock()).isEqualTo(15);
        verify(productRepository).save(product);
        verify(transactionRepository).save(tx);
    }

    @Test
    @DisplayName("Sıfır veya negatif miktarla stok girişi yapılınca hata fırlatılmalı")
    void stockIn_sifirMiktar_hataFirlatir() {
        InventoryTransaction tx = txIn(0, warehouseWithId(1), productWithId(1, 5));

        assertThatThrownBy(() -> service.stockIn(tx, adminUser()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sıfırdan büyük");
    }

    @Test
    @DisplayName("STAFF rolü stok girişi yapınca SecurityException fırlatılmalı")
    void stockIn_staffRolu_guvenlikHatasi() {
        InventoryTransaction tx = txIn(10, warehouseWithId(1), productWithId(1, 5));
        User staff = staffUser(warehouseWithId(1));

        assertThatThrownBy(() -> service.stockIn(tx, staff))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("STAFF");
    }

    @Test
    @DisplayName("Manager kendi deposu dışına stok girişi yapınca SecurityException fırlatılmalı")
    void stockIn_managerYanlisDepo_guvenlikHatasi() {
        Warehouse wh1 = warehouseWithId(1);
        Warehouse wh2 = warehouseWithId(2);
        InventoryTransaction tx = txIn(10, wh2, productWithId(1, 5));
        User manager = managerUser(wh1);

        assertThatThrownBy(() -> service.stockIn(tx, manager))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("kendi deponuza");
    }

    @Test
    @DisplayName("Stok girişi kritik stok seviyesini aşınca bildirim gönderilmeli")
    void stockIn_kritikStoktan_normaleDonunce_bildirimGonderilir() {
        Warehouse wh = warehouseWithId(1);
        Product product = productWithId(1, 3); // LOW_STOCK_THRESHOLD = 5, yani kritik
        InventoryTransaction tx = txIn(10, wh, product);
        User admin = adminUser();
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(transactionRepository.save(tx)).thenReturn(tx);
        User adminNotif = new User(); adminNotif.setId(99);
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(adminNotif));
        when(userRepository.findByRoleAndWarehouse_Id(Role.MANAGER, 1)).thenReturn(List.of());

        service.stockIn(tx, admin);

        verify(notificationService).createNotification(eq(99), eq("Stok Normale Döndü"), anyString(), any(), eq(1));
    }

    // ── stockOut ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Geçerli stok çıkışı kaydedilmeli ve ürün stoğu azalmalı")
    void stockOut_gecerliVeri_stokAzalirVeKaydedilir() {
        Warehouse wh = warehouseWithId(1);
        Product product = productWithId(1, 50);
        InventoryTransaction tx = txOut(10, wh, product);
        User admin = adminUser();
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(transactionRepository.getProductStockInWarehouse(1, 1)).thenReturn(20L);
        when(transactionRepository.save(tx)).thenReturn(tx);

        service.stockOut(tx, admin);

        assertThat(product.getQuantityInStock()).isEqualTo(40);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Sıfır miktarla stok çıkışı yapılınca hata fırlatılmalı")
    void stockOut_sifirMiktar_hataFirlatir() {
        InventoryTransaction tx = txOut(0, warehouseWithId(1), productWithId(1, 10));

        assertThatThrownBy(() -> service.stockOut(tx, adminUser()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sıfırdan büyük");
    }

    @Test
    @DisplayName("Depoda yetersiz stok varken çıkış yapılınca hata fırlatılmalı")
    void stockOut_yetersizStok_hataFirlatir() {
        Warehouse wh = warehouseWithId(1);
        Product product = productWithId(1, 5);
        InventoryTransaction tx = txOut(10, wh, product);
        User admin = adminUser();
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(transactionRepository.getProductStockInWarehouse(1, 1)).thenReturn(3L);

        assertThatThrownBy(() -> service.stockOut(tx, admin))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("yetersiz stok");
    }

    @Test
    @DisplayName("Stok kritik seviyeye düşünce ve bildirim gönderilmemişse bildirim gönderilmeli")
    void stockOut_kritikStokaSeviyesiVeBildirimGonderilmemis_bildirimGonderilir() {
        Warehouse wh = warehouseWithId(1);
        Product product = productWithId(1, 8);
        InventoryTransaction tx = txOut(5, wh, product); // 8-5=3, kritik seviye
        User admin = adminUser();
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(transactionRepository.getProductStockInWarehouse(1, 1)).thenReturn(8L);
        when(transactionRepository.save(tx)).thenReturn(tx);
        when(notificationService.wasLowStockNotifiedRecently(1)).thenReturn(false);
        User adminNotif = new User(); adminNotif.setId(99);
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(adminNotif));
        when(userRepository.findByRoleAndWarehouse_Id(Role.MANAGER, 1)).thenReturn(List.of());

        service.stockOut(tx, admin);

        verify(notificationService).createNotification(eq(99), eq("Kritik Stok Uyarısı"), anyString(), any(), eq(1));
    }

    // ── transfer ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Hedef depo belirtilmeden transfer yapılınca hata fırlatılmalı")
    void transfer_hedefDepoYok_hataFirlatir() {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setQuantity(5);
        tx.setWarehouse(warehouseWithId(1));
        tx.setProduct(productWithId(1, 10));
        tx.setDestinationWarehouse(null);

        assertThatThrownBy(() -> service.transfer(tx, adminUser()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Hedef depo");
    }

    @Test
    @DisplayName("Manager kendi deposu dışından transfer yapınca hata fırlatılmalı")
    void transfer_managerYanlisDepo_hataFirlatir() {
        Warehouse wh1 = warehouseWithId(1);
        Warehouse wh2 = warehouseWithId(2);
        Warehouse wh3 = warehouseWithId(3);
        InventoryTransaction tx = new InventoryTransaction();
        tx.setQuantity(5);
        tx.setWarehouse(wh2);
        tx.setDestinationWarehouse(wh3);
        tx.setProduct(productWithId(1, 10));
        User manager = managerUser(wh1);

        assertThatThrownBy(() -> service.transfer(tx, manager))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("kendi deponuzdan");
    }

    @Test
    @DisplayName("Geçerli transfer kaydedilmeli")
    void transfer_gecerliVeri_kaydedilir() {
        Warehouse src  = warehouseWithId(1);
        Warehouse dest = warehouseWithId(2);
        Product product = productWithId(1, 20);
        InventoryTransaction tx = new InventoryTransaction();
        tx.setQuantity(5);
        tx.setWarehouse(src);
        tx.setDestinationWarehouse(dest);
        tx.setProduct(product);
        User admin = adminUser();
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(transactionRepository.getProductStockInWarehouse(1, 1)).thenReturn(10L);
        when(transactionRepository.save(tx)).thenReturn(tx);

        service.transfer(tx, admin);

        verify(transactionRepository).save(tx);
    }

    // ── yardımcı ──────────────────────────────────────────────────────────────

    private Warehouse warehouseWithId(int id) {
        Warehouse w = new Warehouse(); w.setId(id); return w;
    }

    private Product productWithId(int id, int stock) {
        Product p = new Product(); p.setId(id); p.setQuantityInStock(stock); p.setName("Ürün" + id); return p;
    }

    private InventoryTransaction txIn(int qty, Warehouse wh, Product product) {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setQuantity(qty);
        tx.setWarehouse(wh);
        tx.setProduct(product);
        return tx;
    }

    private InventoryTransaction txOut(int qty, Warehouse wh, Product product) {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setQuantity(qty);
        tx.setWarehouse(wh);
        tx.setProduct(product);
        return tx;
    }

    private User adminUser() {
        User u = new User(); u.setRole(Role.ADMIN); u.setUsername("admin"); return u;
    }

    private User managerUser(Warehouse wh) {
        User u = new User(); u.setRole(Role.MANAGER); u.setWarehouse(wh); u.setUsername("manager"); return u;
    }

    private User staffUser(Warehouse wh) {
        User u = new User(); u.setRole(Role.STAFF); u.setWarehouse(wh); u.setUsername("staff"); return u;
    }
}
