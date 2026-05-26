package org.example.inventoryapi.service;

import org.example.inventoryapi.exception.DeletionBlockedException;
import org.example.inventoryapi.model.entity.*;
import org.example.inventoryapi.model.enums.AssetStatus;
import org.example.inventoryapi.model.enums.Role;
import org.example.inventoryapi.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssetService Testleri")
class AssetServiceTest {

    @Mock AssetRepository            assetRepository;
    @Mock AssetAssignmentRepository  assignmentRepository;
    @Mock AssetTransferRepository    transferRepository;
    @Mock AssetMaintenanceRepository maintenanceRepository;
    @Mock SupplierRepository         supplierRepository;
    @Mock WarehouseRepository        warehouseRepository;
    @InjectMocks AssetService service;

    // ── addAsset ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Geçerli demirbaş eklenince kaydedilmeli")
    void addAsset_gecerliVeri_kaydedilir() {
        Warehouse wh = warehouseWithId(1);
        Asset asset = asset("SN001", "Laptop", wh);
        User admin = adminUser();
        when(assetRepository.existsBySerialNumberIgnoreCase("SN001")).thenReturn(false);
        when(assetRepository.existsByNameIgnoreCase("Laptop")).thenReturn(false);
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(wh));
        when(assetRepository.save(asset)).thenReturn(asset);

        Asset result = service.addAsset(asset, admin);

        assertThat(result).isEqualTo(asset);
        assertThat(result.getStatus()).isEqualTo(AssetStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Seri numarası boş demirbaş eklenince hata fırlatılmalı")
    void addAsset_bosSeriNumarasi_hataFirlatir() {
        Asset asset = asset("", "Laptop", warehouseWithId(1));

        assertThatThrownBy(() -> service.addAsset(asset, adminUser()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Seri numarası");
    }

    @Test
    @DisplayName("Adı boş demirbaş eklenince hata fırlatılmalı")
    void addAsset_bosAd_hataFirlatir() {
        Asset asset = asset("SN001", "", warehouseWithId(1));

        assertThatThrownBy(() -> service.addAsset(asset, adminUser()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Demirbaş adı");
    }

    @Test
    @DisplayName("Mükerrer seri numarasıyla ekleme yapılınca hata fırlatılmalı")
    void addAsset_mukerrerSeriNumarasi_hataFirlatir() {
        Asset asset = asset("SN001", "Laptop", warehouseWithId(1));
        when(assetRepository.existsBySerialNumberIgnoreCase("SN001")).thenReturn(true);

        assertThatThrownBy(() -> service.addAsset(asset, adminUser()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seri numarası");
    }

    @Test
    @DisplayName("Mükerrer demirbaş adıyla ekleme yapılınca hata fırlatılmalı")
    void addAsset_mukerrerAd_hataFirlatir() {
        Asset asset = asset("SN001", "Laptop", warehouseWithId(1));
        when(assetRepository.existsBySerialNumberIgnoreCase("SN001")).thenReturn(false);
        when(assetRepository.existsByNameIgnoreCase("Laptop")).thenReturn(true);

        assertThatThrownBy(() -> service.addAsset(asset, adminUser()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("isimde");
    }

    @Test
    @DisplayName("Gelecek tarihli satın alma tarihiyle ekleme yapılınca hata fırlatılmalı")
    void addAsset_gelecekSatinAlmaTarihi_hataFirlatir() {
        Asset asset = asset("SN001", "Laptop", warehouseWithId(1));
        asset.setPurchaseDate(LocalDate.now().plusDays(1));
        when(assetRepository.existsBySerialNumberIgnoreCase("SN001")).thenReturn(false);
        when(assetRepository.existsByNameIgnoreCase("Laptop")).thenReturn(false);

        assertThatThrownBy(() -> service.addAsset(asset, adminUser()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gelecekte");
    }

    @Test
    @DisplayName("Manager başka depoya demirbaş ekleyince hata fırlatılmalı")
    void addAsset_managerBaskaDipo_hataFirlatir() {
        Warehouse wh1 = warehouseWithId(1);
        Warehouse wh2 = warehouseWithId(2);
        Asset asset = asset("SN001", "Laptop", wh2);
        User manager = managerUser(wh1);
        when(assetRepository.existsBySerialNumberIgnoreCase("SN001")).thenReturn(false);
        when(assetRepository.existsByNameIgnoreCase("Laptop")).thenReturn(false);
        when(warehouseRepository.findById(2)).thenReturn(Optional.of(wh2));

        assertThatThrownBy(() -> service.addAsset(asset, manager))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kendi deponuza");
    }

    // ── assignAsset ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Admin kullanıcısına zimmet yapılınca hata fırlatılmalı")
    void assignAsset_adminHedef_hataFirlatir() {
        User admin = adminUser();
        User targetAdmin = new User(); targetAdmin.setRole(Role.ADMIN);

        assertThatThrownBy(() -> service.assignAsset(1, admin, targetAdmin, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Admin");
    }

    @Test
    @DisplayName("Zaten zimmetli demirbaş tekrar zimmetlenince hata fırlatılmalı")
    void assignAsset_zatenZimmetli_hataFirlatir() {
        User requester = adminUser();
        User target = staffUser(warehouseWithId(1));
        when(assignmentRepository.findByAssetIdAndReturnDateIsNull(1))
                .thenReturn(Optional.of(new AssetAssignment()));

        assertThatThrownBy(() -> service.assignAsset(1, requester, target, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("zimmetli");
    }

    @Test
    @DisplayName("Farklı depodaki kullanıcıya zimmet yapılınca hata fırlatılmalı")
    void assignAsset_farklıDepo_hataFirlatir() {
        Warehouse wh1 = warehouseWithId(1);
        Warehouse wh2 = warehouseWithId(2);
        Asset asset = assetWithStatus(1, AssetStatus.AVAILABLE, wh1);
        User requester = adminUser();
        User target = staffUser(wh2);
        when(assignmentRepository.findByAssetIdAndReturnDateIsNull(1)).thenReturn(Optional.empty());
        when(assetRepository.findById(1)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.assignAsset(1, requester, target, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("aynı depodaki");
    }

    @Test
    @DisplayName("Bakımdaki demirbaş zimmetlenince hata fırlatılmalı")
    void assignAsset_bakimdakiDemirbasci_hataFirlatir() {
        Warehouse wh = warehouseWithId(1);
        Asset asset = assetWithStatus(1, AssetStatus.MAINTENANCE, wh);
        User requester = adminUser();
        User target = staffUser(wh);
        when(assignmentRepository.findByAssetIdAndReturnDateIsNull(1)).thenReturn(Optional.empty());
        when(assetRepository.findById(1)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.assignAsset(1, requester, target, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Bakım");
    }

    @Test
    @DisplayName("Hurda demirbaş zimmetlenince hata fırlatılmalı")
    void assignAsset_hurdaDemirbaski_hataFirlatir() {
        Warehouse wh = warehouseWithId(1);
        Asset asset = assetWithStatus(1, AssetStatus.RETIRED, wh);
        User requester = adminUser();
        User target = staffUser(wh);
        when(assignmentRepository.findByAssetIdAndReturnDateIsNull(1)).thenReturn(Optional.empty());
        when(assetRepository.findById(1)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.assignAsset(1, requester, target, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Hurda");
    }

    @Test
    @DisplayName("Geçerli zimmet atandığında demirbaş IN_USE durumuna geçmeli")
    void assignAsset_gecerliVeri_demirbaskiInUseYapar() {
        Warehouse wh = warehouseWithId(1);
        Asset asset = assetWithStatus(1, AssetStatus.AVAILABLE, wh);
        User requester = adminUser();
        User target = staffUser(wh);
        when(assignmentRepository.findByAssetIdAndReturnDateIsNull(1)).thenReturn(Optional.empty());
        when(assetRepository.findById(1)).thenReturn(Optional.of(asset));

        service.assignAsset(1, requester, target, "not");

        assertThat(asset.getStatus()).isEqualTo(AssetStatus.IN_USE);
        verify(assignmentRepository).save(any(AssetAssignment.class));
        verify(assetRepository).save(asset);
    }

    // ── returnAsset ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Aktif zimmeti olmayan demirbaş iade edilince hata fırlatılmalı")
    void returnAsset_aktifZimmetYok_hataFirlatir() {
        Asset asset = assetWithStatus(1, AssetStatus.IN_USE, warehouseWithId(1));
        when(assetRepository.findById(1)).thenReturn(Optional.of(asset));
        when(assignmentRepository.findByAssetIdAndReturnDateIsNull(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.returnAsset(1, adminUser()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("aktif zimmeti");
    }

    @Test
    @DisplayName("Zimmetli demirbaş iade edilince AVAILABLE durumuna geçmeli")
    void returnAsset_zimmetliDemirbaski_availableYapar() {
        Warehouse wh = warehouseWithId(1);
        Asset asset = assetWithStatus(1, AssetStatus.IN_USE, wh);
        AssetAssignment aa = new AssetAssignment();
        when(assetRepository.findById(1)).thenReturn(Optional.of(asset));
        when(assignmentRepository.findByAssetIdAndReturnDateIsNull(1)).thenReturn(Optional.of(aa));

        service.returnAsset(1, adminUser());

        assertThat(asset.getStatus()).isEqualTo(AssetStatus.AVAILABLE);
        assertThat(aa.getReturnDate()).isNotNull();
    }

    // ── retireAsset ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Zimmetli demirbaş hurdaya ayrılınca hata fırlatılmalı")
    void retireAsset_zimmetliDemirbaski_hataFirlatir() {
        Asset asset = assetWithStatus(1, AssetStatus.IN_USE, warehouseWithId(1));
        when(assetRepository.findById(1)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.retireAsset(1, adminUser()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Zimmetli");
    }

    @Test
    @DisplayName("Boştaki demirbaş hurdaya ayrılınca RETIRED durumuna geçmeli")
    void retireAsset_bostakiDemirbaski_retiredYapar() {
        Asset asset = assetWithStatus(1, AssetStatus.AVAILABLE, warehouseWithId(1));
        when(assetRepository.findById(1)).thenReturn(Optional.of(asset));

        service.retireAsset(1, adminUser());

        assertThat(asset.getStatus()).isEqualTo(AssetStatus.RETIRED);
    }

    // ── transferAsset ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Zimmetli demirbaş transfer edilince hata fırlatılmalı")
    void transferAsset_zimmetliDemirbaski_hataFirlatir() {
        Asset asset = assetWithStatus(1, AssetStatus.IN_USE, warehouseWithId(1));
        when(assetRepository.findById(1)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.transferAsset(1, warehouseWithId(2), "not", adminUser()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Zimmetli");
    }

    @Test
    @DisplayName("Hurda demirbaş transfer edilince hata fırlatılmalı")
    void transferAsset_hurdaDemirbaski_hataFirlatir() {
        Asset asset = assetWithStatus(1, AssetStatus.RETIRED, warehouseWithId(1));
        when(assetRepository.findById(1)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.transferAsset(1, warehouseWithId(2), "not", adminUser()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Hurda");
    }

    @Test
    @DisplayName("Bakımdaki demirbaş transfer edilince hata fırlatılmalı")
    void transferAsset_bakimdakiDemirbaski_hataFirlatir() {
        Asset asset = assetWithStatus(1, AssetStatus.MAINTENANCE, warehouseWithId(1));
        when(assetRepository.findById(1)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.transferAsset(1, warehouseWithId(2), "not", adminUser()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Bakım");
    }

    @Test
    @DisplayName("Geçerli transfer gerçekleşince demirbaşın deposu değişmeli ve kayıt oluşmalı")
    void transferAsset_gecerliVeri_depoGuncellenirKayitOlusur() {
        Warehouse fromWh = warehouseWithId(1);
        Warehouse toWh   = warehouseWithId(2);
        Asset asset = assetWithStatus(1, AssetStatus.AVAILABLE, fromWh);
        when(assetRepository.findById(1)).thenReturn(Optional.of(asset));

        service.transferAsset(1, toWh, "not", adminUser());

        assertThat(asset.getWarehouse()).isEqualTo(toWh);
        verify(transferRepository).save(any(AssetTransfer.class));
        verify(assetRepository).save(asset);
    }

    // ── startMaintenance ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Zimmetli demirbaş bakıma alınınca hata fırlatılmalı")
    void startMaintenance_zimmetliDemirbaski_hataFirlatir() {
        Asset asset = assetWithStatus(1, AssetStatus.IN_USE, warehouseWithId(1));
        when(assetRepository.findById(1)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.startMaintenance(1, "açıklama", "not", adminUser()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Zimmetli");
    }

    @Test
    @DisplayName("Zaten bakımdaki demirbaş tekrar bakıma alınınca hata fırlatılmalı")
    void startMaintenance_zatenBakimda_hataFirlatir() {
        Asset asset = assetWithStatus(1, AssetStatus.MAINTENANCE, warehouseWithId(1));
        when(assetRepository.findById(1)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.startMaintenance(1, "açıklama", "not", adminUser()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("zaten bakımda");
    }

    @Test
    @DisplayName("Hurda demirbaş bakıma alınınca hata fırlatılmalı")
    void startMaintenance_hurdaDemirbaski_hataFirlatir() {
        Asset asset = assetWithStatus(1, AssetStatus.RETIRED, warehouseWithId(1));
        when(assetRepository.findById(1)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.startMaintenance(1, "açıklama", "not", adminUser()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Hurda");
    }

    @Test
    @DisplayName("Geçerli bakım başlatılınca demirbaş MAINTENANCE durumuna geçmeli")
    void startMaintenance_gecerliVeri_maintenanceYapar() {
        Asset asset = assetWithStatus(1, AssetStatus.AVAILABLE, warehouseWithId(1));
        when(assetRepository.findById(1)).thenReturn(Optional.of(asset));

        service.startMaintenance(1, "Arıza", "not", adminUser());

        assertThat(asset.getStatus()).isEqualTo(AssetStatus.MAINTENANCE);
        verify(maintenanceRepository).save(any(AssetMaintenance.class));
    }

    // ── endMaintenance ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Aktif bakım kaydı olmayan demirbaşın bakımı bitirilince hata fırlatılmalı")
    void endMaintenance_aktifBakimKaydiYok_hataFirlatir() {
        Asset asset = assetWithStatus(1, AssetStatus.MAINTENANCE, warehouseWithId(1));
        when(assetRepository.findById(1)).thenReturn(Optional.of(asset));
        when(maintenanceRepository.findByAssetIdAndEndDateIsNull(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.endMaintenance(1, adminUser()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Aktif bakım");
    }

    @Test
    @DisplayName("Bakım bitirilince demirbaş AVAILABLE durumuna geçmeli")
    void endMaintenance_aktifBakimVar_availableYapar() {
        Asset asset = assetWithStatus(1, AssetStatus.MAINTENANCE, warehouseWithId(1));
        when(assetRepository.findById(1)).thenReturn(Optional.of(asset));
        when(maintenanceRepository.findByAssetIdAndEndDateIsNull(1))
                .thenReturn(Optional.of(new AssetMaintenance()));

        service.endMaintenance(1, adminUser());

        assertThat(asset.getStatus()).isEqualTo(AssetStatus.AVAILABLE);
        verify(maintenanceRepository).closeActiveByAssetId(1);
    }

    // ── deleteAsset ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Zimmetli demirbaş silinince DeletionBlockedException fırlatılmalı")
    void deleteAsset_zimmetliDemirbaski_engellenir() {
        Asset asset = assetWithStatus(1, AssetStatus.IN_USE, warehouseWithId(1));
        when(assetRepository.findById(1)).thenReturn(Optional.of(asset));
        AssetAssignment aa = new AssetAssignment();
        User u = new User(); u.setUsername("ali"); aa.setUser(u);
        when(assignmentRepository.findByAssetIdAndReturnDateIsNull(1)).thenReturn(Optional.of(aa));

        assertThatThrownBy(() -> service.deleteAsset(1))
                .isInstanceOf(DeletionBlockedException.class)
                .hasMessageContaining("zimmetli");
    }

    @Test
    @DisplayName("Bakımdaki demirbaş silinince DeletionBlockedException fırlatılmalı")
    void deleteAsset_bakimdakiDemirbaski_engellenir() {
        Asset asset = assetWithStatus(1, AssetStatus.MAINTENANCE, warehouseWithId(1));
        when(assetRepository.findById(1)).thenReturn(Optional.of(asset));
        when(assignmentRepository.findByAssetIdAndReturnDateIsNull(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteAsset(1))
                .isInstanceOf(DeletionBlockedException.class)
                .hasMessageContaining("bakım");
    }

    @Test
    @DisplayName("Boştaki demirbaş başarıyla silinmeli")
    void deleteAsset_bostakiDemirbaski_silmeBasarili() {
        Asset asset = assetWithStatus(1, AssetStatus.AVAILABLE, warehouseWithId(1));
        when(assetRepository.findById(1)).thenReturn(Optional.of(asset));
        when(assignmentRepository.findByAssetIdAndReturnDateIsNull(1)).thenReturn(Optional.empty());

        assertThatNoException().isThrownBy(() -> service.deleteAsset(1));
        verify(assetRepository).deleteById(1);
    }

    // ── yardımcı ──────────────────────────────────────────────────────────────

    private Asset asset(String serial, String name, Warehouse wh) {
        Asset a = new Asset();
        a.setSerialNumber(serial);
        a.setName(name);
        a.setWarehouse(wh);
        return a;
    }

    private Asset assetWithStatus(int id, AssetStatus status, Warehouse wh) {
        Asset a = new Asset();
        a.setId(id);
        a.setStatus(status);
        a.setWarehouse(wh);
        a.setSerialNumber("SN" + id);
        a.setName("Asset" + id);
        return a;
    }

    private Warehouse warehouseWithId(int id) {
        Warehouse w = new Warehouse(); w.setId(id); return w;
    }

    private User adminUser() {
        User u = new User(); u.setRole(Role.ADMIN); return u;
    }

    private User managerUser(Warehouse wh) {
        User u = new User(); u.setRole(Role.MANAGER); u.setWarehouse(wh); return u;
    }

    private User staffUser(Warehouse wh) {
        User u = new User(); u.setRole(Role.STAFF); u.setWarehouse(wh); return u;
    }
}
