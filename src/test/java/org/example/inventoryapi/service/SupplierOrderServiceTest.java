package org.example.inventoryapi.service;

import org.example.inventoryapi.model.entity.*;
import org.example.inventoryapi.model.enums.Role;
import org.example.inventoryapi.model.enums.SupplierOrderStatus;
import org.example.inventoryapi.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupplierOrderService Testleri")
class SupplierOrderServiceTest {

    @Mock SupplierOrderRepository        orderRepository;
    @Mock ProductRepository              productRepository;
    @Mock SupplierRepository             supplierRepository;
    @Mock WarehouseRepository            warehouseRepository;
    @Mock InventoryTransactionRepository transactionRepository;
    @Mock UserRepository                 userRepository;
    @Mock NotificationService            notificationService;
    @InjectMocks SupplierOrderService service;

    // ── createOrder ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Geçerli sipariş oluşturulunca BEKLIYOR durumunda kaydedilmeli")
    void createOrder_gecerliVeri_bekleyorDurumundaKaydedilir() {
        Supplier supplier = supplierWithId(1);
        Product  product  = productWithSupplier(1, supplier);
        Warehouse wh      = warehouseWithId(1);

        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(wh));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(userRepository.findByRoleAndSupplier_Id(Role.SUPPLIER, 1)).thenReturn(List.of());
        when(orderRepository.save(any())).thenAnswer(inv -> {
            SupplierOrder o = inv.getArgument(0); o.setId(1); return o;
        });

        SupplierOrder result = service.createOrder(1, 1, 1, 10, "admin");

        assertThat(result.getStatus()).isEqualTo(SupplierOrderStatus.BEKLIYOR);
        assertThat(result.getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("Bulunamayan ürünle sipariş oluşturulunca hata fırlatılmalı")
    void createOrder_urunBulunamadi_hataFirlatir() {
        when(productRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrder(99, 1, 1, 5, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ürün bulunamadı");
    }

    @Test
    @DisplayName("Bulunamayan tedarikçiyle sipariş oluşturulunca hata fırlatılmalı")
    void createOrder_tedarikciBuilunamadi_hataFirlatir() {
        when(productRepository.findById(1)).thenReturn(Optional.of(productWithSupplier(1, supplierWithId(2))));
        when(supplierRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrder(1, 99, 1, 5, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tedarikçi bulunamadı");
    }

    @Test
    @DisplayName("Ürün tedarikçiye ait değilse hata fırlatılmalı")
    void createOrder_urunTedarikciEAit_degil_hataFirlatir() {
        Supplier supplierA = supplierWithId(1);
        Supplier supplierB = supplierWithId(2);
        Product product = productWithSupplier(1, supplierA); // ürün supplierA'ya ait
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(supplierRepository.findById(2)).thenReturn(Optional.of(supplierB)); // supplierB ile sipariş
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(warehouseWithId(1)));

        assertThatThrownBy(() -> service.createOrder(1, 2, 1, 5, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tedarikçiye ait değil");
    }

    @Test
    @DisplayName("Sıfır veya negatif miktarla sipariş oluşturulunca hata fırlatılmalı")
    void createOrder_sifirMiktar_hataFirlatir() {
        Supplier supplier = supplierWithId(1);
        Product product = productWithSupplier(1, supplier);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(warehouseWithId(1)));

        assertThatThrownBy(() -> service.createOrder(1, 1, 1, 0, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sıfırdan büyük");
    }

    @Test
    @DisplayName("Sipariş oluşturulunca tedarikçi kullanıcılarına bildirim gönderilmeli")
    void createOrder_gecerliVeri_tedarikciKullanicilarinabildiriGonderilir() {
        Supplier supplier = supplierWithId(1);
        Product  product  = productWithSupplier(1, supplier);
        User supplierUser = new User(); supplierUser.setId(55);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(warehouseWithId(1)));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(userRepository.findByRoleAndSupplier_Id(Role.SUPPLIER, 1)).thenReturn(List.of(supplierUser));
        when(orderRepository.save(any())).thenAnswer(inv -> { SupplierOrder o = inv.getArgument(0); o.setId(1); return o; });

        service.createOrder(1, 1, 1, 10, "admin");

        verify(notificationService).createNotification(eq(55), eq("Yeni Sipariş"), anyString(), any(), eq(1));
    }

    // ── updateStatus — ONAYLANDI ──────────────────────────────────────────────

    @Test
    @DisplayName("BEKLIYOR siparişi ADMIN tarafından onaylanabilmeli")
    void updateStatus_bekleyenSiparisAdminOnaylar_onayliyorDurumunGecer() {
        SupplierOrder order = orderWithStatus(1, SupplierOrderStatus.BEKLIYOR, supplierWithId(1));
        User admin = userWithRole(Role.ADMIN);
        when(orderRepository.findByIdWithLock(1)).thenReturn(Optional.of(order));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SupplierOrder result = service.updateStatus(1, SupplierOrderStatus.ONAYLANDI, "admin");

        assertThat(result.getStatus()).isEqualTo(SupplierOrderStatus.ONAYLANDI);
    }

    @Test
    @DisplayName("BEKLIYOR siparişi kendi tedarikçi kullanıcısı tarafından onaylanabilmeli")
    void updateStatus_bekleyenSiparisSupplierOnaylar_onayliDurumunGecer() {
        Supplier supplier = supplierWithId(1);
        SupplierOrder order = orderWithStatus(1, SupplierOrderStatus.BEKLIYOR, supplier);
        User supplierUser = supplierUserWithSupplier(supplier);
        when(orderRepository.findByIdWithLock(1)).thenReturn(Optional.of(order));
        when(userRepository.findByUsername("supplier")).thenReturn(Optional.of(supplierUser));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByRole(any())).thenReturn(List.of());

        SupplierOrder result = service.updateStatus(1, SupplierOrderStatus.ONAYLANDI, "supplier");

        assertThat(result.getStatus()).isEqualTo(SupplierOrderStatus.ONAYLANDI);
    }

    @Test
    @DisplayName("STAFF rolü onaylama işlemi yapınca hata fırlatılmalı")
    void updateStatus_staffOnaylamaya_calisiyor_hataFirlatir() {
        SupplierOrder order = orderWithStatus(1, SupplierOrderStatus.BEKLIYOR, supplierWithId(1));
        User staff = userWithRole(Role.STAFF);
        when(orderRepository.findByIdWithLock(1)).thenReturn(Optional.of(order));
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));

        assertThatThrownBy(() -> service.updateStatus(1, SupplierOrderStatus.ONAYLANDI, "staff"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tedarikçi veya admin");
    }

    @Test
    @DisplayName("BEKLIYOR olmayan sipariş onaylanmaya çalışılınca hata fırlatılmalı")
    void updateStatus_onayliSiparis_tekrarOnaylanamaz() {
        SupplierOrder order = orderWithStatus(1, SupplierOrderStatus.ONAYLANDI, supplierWithId(1));
        User admin = userWithRole(Role.ADMIN);
        when(orderRepository.findByIdWithLock(1)).thenReturn(Optional.of(order));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.updateStatus(1, SupplierOrderStatus.ONAYLANDI, "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BEKLIYOR");
    }

    @Test
    @DisplayName("Başka tedarikçinin siparişini onaylamaya çalışınca hata fırlatılmalı")
    void updateStatus_baskaTedarikcininsiparis_sahiplikHatasi() {
        Supplier supplier1 = supplierWithId(1);
        Supplier supplier2 = supplierWithId(2);
        SupplierOrder order = orderWithStatus(1, SupplierOrderStatus.BEKLIYOR, supplier1);
        User supplierUser = supplierUserWithSupplier(supplier2);
        when(orderRepository.findByIdWithLock(1)).thenReturn(Optional.of(order));
        when(userRepository.findByUsername("supplier")).thenReturn(Optional.of(supplierUser));

        assertThatThrownBy(() -> service.updateStatus(1, SupplierOrderStatus.ONAYLANDI, "supplier"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Bu sipariş size ait değil");
    }

    // ── updateStatus — YOLDA ─────────────────────────────────────────────────

    @Test
    @DisplayName("ONAYLANDI siparişi YOLDA durumuna geçirilmeli")
    void updateStatus_onayliSiparis_yoldaDurumunagecilebilir() {
        Supplier supplier = supplierWithId(1);
        SupplierOrder order = orderWithStatus(1, SupplierOrderStatus.ONAYLANDI, supplier);
        User supplierUser = supplierUserWithSupplier(supplier);
        order.setWarehouse(warehouseWithId(1));
        when(orderRepository.findByIdWithLock(1)).thenReturn(Optional.of(order));
        when(userRepository.findByUsername("supplier")).thenReturn(Optional.of(supplierUser));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByRole(any())).thenReturn(List.of());
        when(userRepository.findByRoleAndWarehouse_Id(any(), anyInt())).thenReturn(List.of());

        SupplierOrder result = service.updateStatus(1, SupplierOrderStatus.YOLDA, "supplier");

        assertThat(result.getStatus()).isEqualTo(SupplierOrderStatus.YOLDA);
    }

    @Test
    @DisplayName("BEKLIYOR siparişi doğrudan YOLDA yapılamaz")
    void updateStatus_bekleyenSiparis_direk_yoldaYapilamaz() {
        SupplierOrder order = orderWithStatus(1, SupplierOrderStatus.BEKLIYOR, supplierWithId(1));
        User admin = userWithRole(Role.ADMIN);
        when(orderRepository.findByIdWithLock(1)).thenReturn(Optional.of(order));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.updateStatus(1, SupplierOrderStatus.YOLDA, "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ONAYLANDI");
    }

    // ── updateStatus — TESLIM_ALINDI ──────────────────────────────────────────

    @Test
    @DisplayName("YOLDA siparişi ADMIN/MANAGER tarafından teslim alınabilmeli ve stok artmalı")
    void updateStatus_yoldaSiparis_teslimAlinir_stokArtar() {
        Supplier supplier = supplierWithId(1);
        Product  product  = productWithSupplier(1, supplier); product.setQuantityInStock(10);
        Warehouse wh = warehouseWithId(1);
        SupplierOrder order = orderWithStatus(1, SupplierOrderStatus.YOLDA, supplier);
        order.setProduct(product);
        order.setWarehouse(wh);
        order.setQuantity(20);
        User manager = userWithRole(Role.MANAGER);
        when(orderRepository.findByIdWithLock(1)).thenReturn(Optional.of(order));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateStatus(1, SupplierOrderStatus.TESLIM_ALINDI, "manager");

        verify(transactionRepository).save(any(InventoryTransaction.class));
        verify(productRepository).incrementStock(1, 20);
    }

    @Test
    @DisplayName("STAFF rolü teslim alma işlemi yapınca hata fırlatılmalı")
    void updateStatus_staffTeslimAlmaya_calisiyor_hataFirlatir() {
        SupplierOrder order = orderWithStatus(1, SupplierOrderStatus.YOLDA, supplierWithId(1));
        User staff = userWithRole(Role.STAFF);
        when(orderRepository.findByIdWithLock(1)).thenReturn(Optional.of(order));
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));

        assertThatThrownBy(() -> service.updateStatus(1, SupplierOrderStatus.TESLIM_ALINDI, "staff"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("yönetici");
    }

    // ── updateProductPrice ────────────────────────────────────────────────────

    @Test
    @DisplayName("Tedarikçi kendi ürününün fiyatını güncelleyebilmeli")
    void updateProductPrice_kendineAitUrun_fiyatGuncellenir() {
        Supplier supplier = supplierWithId(1);
        Product product = productWithSupplier(1, supplier);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        Product result = service.updateProductPrice(1, BigDecimal.valueOf(99.99), 1);

        assertThat(result.getPrice()).isEqualByComparingTo("99.99");
    }

    @Test
    @DisplayName("Tedarikçi başka tedarikçinin ürününü güncelleyince hata fırlatılmalı")
    void updateProductPrice_baskaTedarikcinin_urunu_hataFirlatir() {
        Supplier supplier1 = supplierWithId(1);
        Product product = productWithSupplier(1, supplier1);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.updateProductPrice(1, BigDecimal.valueOf(50), 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size ait değil");
    }

    @Test
    @DisplayName("Negatif fiyatla güncelleme yapılınca hata fırlatılmalı")
    void updateProductPrice_negatifFiyat_hataFirlatir() {
        Supplier supplier = supplierWithId(1);
        Product product = productWithSupplier(1, supplier);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.updateProductPrice(1, BigDecimal.valueOf(-1), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sıfırdan küçük");
    }

    // ── yardımcı ──────────────────────────────────────────────────────────────

    private Supplier supplierWithId(int id) {
        Supplier s = new Supplier(); s.setId(id); s.setName("Tedarikçi" + id); return s;
    }

    private Warehouse warehouseWithId(int id) {
        Warehouse w = new Warehouse(); w.setId(id); return w;
    }

    private Product productWithSupplier(int id, Supplier supplier) {
        Product p = new Product();
        p.setId(id);
        p.setName("Ürün" + id);
        p.setSuppliers(Set.of(supplier));
        return p;
    }

    private User userWithRole(Role role) {
        User u = new User(); u.setRole(role); u.setUsername(role.name().toLowerCase()); u.setId(role.ordinal() + 1); return u;
    }

    private User supplierUserWithSupplier(Supplier supplier) {
        User u = new User();
        u.setRole(Role.SUPPLIER);
        u.setUsername("supplier");
        u.setId(100);
        u.setSupplier(supplier);
        return u;
    }

    private SupplierOrder orderWithStatus(int id, SupplierOrderStatus status, Supplier supplier) {
        SupplierOrder o = new SupplierOrder();
        o.setId(id);
        o.setStatus(status);
        o.setSupplier(supplier);
        o.setQuantity(10);
        Product p = new Product(); p.setId(1); p.setName("Ürün1"); p.setQuantityInStock(5);
        o.setProduct(p);
        return o;
    }
}
