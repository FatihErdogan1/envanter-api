package org.example.inventoryapi.service;

import org.example.inventoryapi.model.entity.*;
import org.example.inventoryapi.model.enums.Role;
import org.example.inventoryapi.model.enums.StockRequestStatus;
import org.example.inventoryapi.repository.InventoryTransactionRepository;
import org.example.inventoryapi.repository.ProductRepository;
import org.example.inventoryapi.repository.StockRequestRepository;
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
@DisplayName("StockRequestService Testleri")
class StockRequestServiceTest {

    @Mock StockRequestRepository         stockRequestRepository;
    @Mock ProductRepository              productRepository;
    @Mock InventoryTransactionRepository transactionRepository;
    @Mock UserRepository                 userRepository;
    @Mock NotificationService            notificationService;
    @InjectMocks StockRequestService service;

    // ── getRequestsFor ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Admin tüm talepleri görebilmeli")
    void getRequestsFor_adminRolu_tumTalepleriDondurir() {
        User admin = userWithRole(Role.ADMIN);
        when(stockRequestRepository.findAllByOrderByRequestDateDesc()).thenReturn(List.of(new StockRequest()));

        List<StockRequest> result = service.getRequestsFor(admin);

        assertThat(result).hasSize(1);
        verify(stockRequestRepository).findAllByOrderByRequestDateDesc();
    }

    @Test
    @DisplayName("Manager tüm talepleri görebilmeli")
    void getRequestsFor_managerRolu_tumTalepleriDondurir() {
        User manager = userWithRole(Role.MANAGER);
        when(stockRequestRepository.findAllByOrderByRequestDateDesc()).thenReturn(List.of(new StockRequest(), new StockRequest()));

        List<StockRequest> result = service.getRequestsFor(manager);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Staff yalnızca kendi taleplerini görebilmeli")
    void getRequestsFor_staffRolu_sadecKendiTalepleri() {
        User staff = userWithRole(Role.STAFF);
        staff.setUsername("ali");
        when(stockRequestRepository.findByRequestedByUsernameOrderByRequestDateDesc("ali"))
                .thenReturn(List.of(new StockRequest()));

        List<StockRequest> result = service.getRequestsFor(staff);

        assertThat(result).hasSize(1);
        verify(stockRequestRepository).findByRequestedByUsernameOrderByRequestDateDesc("ali");
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Geçerli stok talebi oluşturulunca kaydedilmeli ve bildirim gönderilmeli")
    void create_gecerliVeri_kaydedilirVeBildirimGonderilir() {
        Product product   = productWithId(1);
        Warehouse wh      = warehouseWithId(1);
        User requester    = userWithRole(Role.STAFF);
        User manager      = userWithId(10);
        when(stockRequestRepository.save(any())).thenAnswer(inv -> {
            StockRequest r = inv.getArgument(0); r.setId(1); return r;
        });
        when(userRepository.findByRoleAndWarehouse_Id(Role.MANAGER, 1)).thenReturn(List.of(manager));

        StockRequest result = service.create(product, wh, 5, "not", requester);

        assertThat(result.getStatus()).isEqualTo(StockRequestStatus.PENDING);
        verify(notificationService).createNotification(eq(10), eq("Yeni Stok Talebi"), anyString(), any(), eq(1));
    }

    @Test
    @DisplayName("Sıfır veya negatif miktarla talep oluşturulunca hata fırlatılmalı")
    void create_sifirMiktar_hataFirlatir() {
        assertThatThrownBy(() -> service.create(productWithId(1), warehouseWithId(1), 0, null, userWithRole(Role.STAFF)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sıfırdan büyük");
    }

    // ── approve ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Bekleyen talep onaylanınca stok artmalı ve işlem kaydı oluşmalı")
    void approve_bekleyenTalep_stokArtarVeKayitOlusur() {
        Product product  = productWithId(1); product.setQuantityInStock(10);
        StockRequest req = pendingRequest(1, product, warehouseWithId(1), userWithId(5));
        User reviewer    = userWithId(99);
        when(stockRequestRepository.findById(1)).thenReturn(Optional.of(req));
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(stockRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StockRequest result = service.approve(1, reviewer, "Onaylandı");

        assertThat(result.getStatus()).isEqualTo(StockRequestStatus.APPROVED);
        assertThat(product.getQuantityInStock()).isEqualTo(10 + req.getQuantity());
        verify(transactionRepository).save(any(InventoryTransaction.class));
    }

    @Test
    @DisplayName("Onaylanan talep sahibi farklıysa bildirim gönderilmeli")
    void approve_farkliKullanici_bildirimGonderilir() {
        User requester = userWithId(5);
        Product product = productWithId(1); product.setQuantityInStock(10);
        StockRequest req = pendingRequest(1, product, warehouseWithId(1), requester);
        User reviewer = userWithId(99);
        when(stockRequestRepository.findById(1)).thenReturn(Optional.of(req));
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(stockRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.approve(1, reviewer, null);

        verify(notificationService).createNotification(eq(5), eq("Stok Talebiniz Onaylandı"), anyString(), any(), eq(1));
    }

    @Test
    @DisplayName("Zaten sonuçlandırılmış talep tekrar onaylanınca hata fırlatılmalı")
    void approve_sonuclandirilmisTalep_hataFirlatir() {
        StockRequest req = new StockRequest(); req.setStatus(StockRequestStatus.APPROVED);
        when(stockRequestRepository.findById(1)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> service.approve(1, userWithId(99), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sonuçlandırılmış");
    }

    // ── reject ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Bekleyen talep reddedilince REJECTED durumuna geçmeli")
    void reject_bekleyenTalep_rejectedYapar() {
        StockRequest req = pendingRequest(1, productWithId(1), warehouseWithId(1), userWithId(5));
        User reviewer = userWithId(99);
        when(stockRequestRepository.findById(1)).thenReturn(Optional.of(req));
        when(stockRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StockRequest result = service.reject(1, reviewer, "Uygun değil");

        assertThat(result.getStatus()).isEqualTo(StockRequestStatus.REJECTED);
    }

    @Test
    @DisplayName("Reddedilen talep sahibi farklıysa bildirim gönderilmeli")
    void reject_farkliKullanici_bildirimGonderilir() {
        User requester = userWithId(5);
        Product product = productWithId(1);
        StockRequest req = pendingRequest(1, product, warehouseWithId(1), requester);
        User reviewer = userWithId(99);
        when(stockRequestRepository.findById(1)).thenReturn(Optional.of(req));
        when(stockRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.reject(1, reviewer, "Uygun değil");

        verify(notificationService).createNotification(eq(5), eq("Stok Talebiniz Reddedildi"), anyString(), any(), eq(1));
    }

    @Test
    @DisplayName("Zaten sonuçlandırılmış talep reddedilince hata fırlatılmalı")
    void reject_sonuclandirilmisTalep_hataFirlatir() {
        StockRequest req = new StockRequest(); req.setStatus(StockRequestStatus.REJECTED);
        when(stockRequestRepository.findById(1)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> service.reject(1, userWithId(99), null))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── yardımcı ──────────────────────────────────────────────────────────────

    private User userWithRole(Role role) {
        User u = new User(); u.setRole(role); u.setId(1); return u;
    }

    private User userWithId(int id) {
        User u = new User(); u.setId(id); u.setRole(Role.STAFF); return u;
    }

    private Product productWithId(int id) {
        Product p = new Product(); p.setId(id); p.setName("Ürün" + id); return p;
    }

    private Warehouse warehouseWithId(int id) {
        Warehouse w = new Warehouse(); w.setId(id); return w;
    }

    private StockRequest pendingRequest(int id, Product product, Warehouse wh, User requester) {
        StockRequest r = new StockRequest();
        r.setId(id);
        r.setProduct(product);
        r.setWarehouse(wh);
        r.setQuantity(5);
        r.setStatus(StockRequestStatus.PENDING);
        r.setRequestedBy(requester);
        return r;
    }
}
