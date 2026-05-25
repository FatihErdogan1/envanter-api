package org.example.inventoryapi.service;

import org.example.inventoryapi.exception.DeletionBlockedException;
import org.example.inventoryapi.model.entity.*;
import org.example.inventoryapi.model.enums.AssetStatus;
import org.example.inventoryapi.model.enums.Role;
import org.example.inventoryapi.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AssetService {

    private final AssetRepository            assetRepository;
    private final AssetAssignmentRepository  assignmentRepository;
    private final AssetTransferRepository    transferRepository;
    private final AssetMaintenanceRepository maintenanceRepository;
    private final SupplierRepository         supplierRepository;
    private final WarehouseRepository        warehouseRepository;

    public AssetService(AssetRepository assetRepository,
                        AssetAssignmentRepository assignmentRepository,
                        AssetTransferRepository transferRepository,
                        AssetMaintenanceRepository maintenanceRepository,
                        SupplierRepository supplierRepository,
                        WarehouseRepository warehouseRepository) {
        this.assetRepository      = assetRepository;
        this.assignmentRepository = assignmentRepository;
        this.transferRepository   = transferRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.supplierRepository   = supplierRepository;
        this.warehouseRepository  = warehouseRepository;
    }

    public List<Asset> getAllAssets(User requestingUser) {
        List<Asset> assets;
        if (requestingUser.getRole() != Role.ADMIN) {
            assets = requestingUser.getWarehouse() != null
                    ? assetRepository.findByWarehouse_Id(requestingUser.getWarehouse().getId())
                    : List.of();
        } else {
            assets = assetRepository.findAll();
        }
        assets.forEach(a -> assignmentRepository
                .findByAssetIdAndReturnDateIsNull(a.getId())
                .ifPresent(aa -> a.setAssignedToUsername(aa.getUser().getUsername())));
        return assets;
    }

    public Asset addAsset(Asset asset, User requestingUser) {
        if (asset.getSerialNumber() == null || asset.getSerialNumber().isBlank())
            throw new IllegalArgumentException("Seri numarası zorunludur.");
        if (asset.getName() == null || asset.getName().isBlank())
            throw new IllegalArgumentException("Demirbaş adı zorunludur.");
        if (asset.getWarehouse() == null || asset.getWarehouse().getId() <= 0)
            throw new IllegalArgumentException("Depo seçilmelidir.");
        if (assetRepository.existsBySerialNumberIgnoreCase(asset.getSerialNumber()))
            throw new IllegalArgumentException("Bu seri numarası zaten kayıtlı.");
        if (assetRepository.existsByNameIgnoreCase(asset.getName()))
            throw new IllegalArgumentException("Bu isimde bir demirbaş zaten kayıtlı.");
        if (asset.getPurchaseDate() != null && asset.getPurchaseDate().isAfter(java.time.LocalDate.now()))
            throw new IllegalArgumentException("Satın alma tarihi gelecekte olamaz.");

        Warehouse warehouse = warehouseRepository.findById(asset.getWarehouse().getId())
                .orElseThrow(() -> new IllegalArgumentException("Depo bulunamadı."));
        asset.setWarehouse(warehouse);

        if (requestingUser.getRole() != Role.ADMIN) {
            if (requestingUser.getWarehouse() == null
                    || requestingUser.getWarehouse().getId() != warehouse.getId())
                throw new IllegalArgumentException("Yalnızca kendi deponuza demirbaş ekleyebilirsiniz.");
        }

        if (asset.getSupplier() != null && asset.getSupplier().getId() > 0) {
            Supplier supplier = supplierRepository.findById(asset.getSupplier().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Tedarikçi bulunamadı."));
            asset.setSupplier(supplier);
        } else {
            asset.setSupplier(null);
        }

        if (asset.getStatus() == null) asset.setStatus(AssetStatus.AVAILABLE);
        return assetRepository.save(asset);
    }

    public Asset updateAsset(int id, Asset updated) {
        Asset existing = getOrThrow(id);
        if (assetRepository.existsByNameIgnoreCaseAndIdNot(updated.getName(), id))
            throw new IllegalArgumentException("Bu isimde bir demirbaş zaten kayıtlı.");
        if (assetRepository.existsBySerialNumberIgnoreCase(updated.getSerialNumber())
                && !existing.getSerialNumber().equalsIgnoreCase(updated.getSerialNumber()))
            throw new IllegalArgumentException("Bu seri numarası zaten kayıtlı.");
        existing.setSerialNumber(updated.getSerialNumber());
        existing.setName(updated.getName());
        existing.setSupplier(updated.getSupplier());
        existing.setWarehouse(updated.getWarehouse());
        existing.setPurchaseDate(updated.getPurchaseDate());
        existing.setStatus(updated.getStatus());
        return assetRepository.save(existing);
    }

    public void deleteAsset(int id) {
        Asset asset = getOrThrow(id);
        assignmentRepository.findByAssetIdAndReturnDateIsNull(id).ifPresent(aa -> {
            String username = aa.getUser() != null ? aa.getUser().getUsername() : "bir kullanıcı";
            throw new DeletionBlockedException(
                "Bu demirbaş " + username + " kullanıcısına zimmetli. Önce zimmeti düşürün.",
                1, "zimmet");
        });
        if (asset.getStatus() == AssetStatus.MAINTENANCE)
            throw new DeletionBlockedException(
                "Bu demirbaş bakımda. Önce bakımı tamamlayın.",
                1, "bakım");
        assetRepository.deleteById(id);
    }

    @Transactional
    public void assignAsset(int assetId, User requestingUser, User targetUser, String notes) {
        if (targetUser.getRole() == Role.ADMIN)
            throw new IllegalArgumentException("Admin rolündeki kullanıcılara zimmet yapılamaz.");
        if (assignmentRepository.findByAssetIdAndReturnDateIsNull(assetId).isPresent())
            throw new IllegalStateException("Bu demirbaş zaten zimmetli.");
        Asset asset = getOrThrow(assetId);
        checkWarehouseAccess(requestingUser, asset);
        if (targetUser.getWarehouse() == null || asset.getWarehouse() == null
                || targetUser.getWarehouse().getId() != asset.getWarehouse().getId())
            throw new IllegalStateException("Demirbaş sadece aynı depodaki kullanıcıya zimmetlenebilir.");
        if (asset.getStatus() == AssetStatus.MAINTENANCE) throw new IllegalStateException("Bakımdaki demirbaş zimmetlenemez.");
        if (asset.getStatus() == AssetStatus.RETIRED) throw new IllegalStateException("Hurda demirbaş zimmetlenemez.");

        AssetAssignment aa = new AssetAssignment();
        aa.setAssetId(assetId);
        aa.setUser(targetUser);
        aa.setAssignedDate(LocalDateTime.now());
        aa.setNotes(notes);
        assignmentRepository.save(aa);
        asset.setStatus(AssetStatus.IN_USE);
        assetRepository.save(asset);
    }

    @Transactional
    public void returnAsset(int assetId, User requestingUser) {
        Asset asset = getOrThrow(assetId);
        checkWarehouseAccess(requestingUser, asset);
        AssetAssignment aa = assignmentRepository.findByAssetIdAndReturnDateIsNull(assetId)
                .orElseThrow(() -> new IllegalStateException("Bu demirbaşın aktif zimmeti yok."));
        aa.setReturnDate(LocalDateTime.now());
        assignmentRepository.save(aa);
        asset.setStatus(AssetStatus.AVAILABLE);
        assetRepository.save(asset);
    }

    public void retireAsset(int assetId, User requestingUser) {
        Asset asset = getOrThrow(assetId);
        checkWarehouseAccess(requestingUser, asset);
        if (asset.getStatus() == AssetStatus.IN_USE)
            throw new IllegalStateException("Zimmetli demirbaş hurdaya ayrılamaz. Önce zimmeti düşürün.");
        asset.setStatus(AssetStatus.RETIRED);
        assetRepository.save(asset);
    }

    @Transactional
    public void transferAsset(int assetId, Warehouse toWarehouse, String notes, User requestingUser) {
        Asset asset = getOrThrow(assetId);
        checkWarehouseAccess(requestingUser, asset);
        if (asset.getStatus() == AssetStatus.IN_USE) throw new IllegalStateException("Zimmetli demirbaş transfer edilemez.");
        if (asset.getStatus() == AssetStatus.RETIRED) throw new IllegalStateException("Hurda demirbaş transfer edilemez.");
        if (asset.getStatus() == AssetStatus.MAINTENANCE) throw new IllegalStateException("Bakımdaki demirbaş transfer edilemez.");

        AssetTransfer transfer = new AssetTransfer();
        transfer.setAssetId(assetId);
        transfer.setFromWarehouse(asset.getWarehouse());
        transfer.setToWarehouse(toWarehouse);
        transfer.setTransferDate(LocalDateTime.now());
        transfer.setNotes(notes);
        transferRepository.save(transfer);
        asset.setWarehouse(toWarehouse);
        assetRepository.save(asset);
    }

    @Transactional
    public void startMaintenance(int assetId, String description, String notes, User requestingUser) {
        Asset asset = getOrThrow(assetId);
        checkWarehouseAccess(requestingUser, asset);
        if (asset.getStatus() == AssetStatus.IN_USE) throw new IllegalStateException("Zimmetli demirbaş bakıma alınamaz.");
        if (asset.getStatus() == AssetStatus.MAINTENANCE) throw new IllegalStateException("Demirbaş zaten bakımda.");
        if (asset.getStatus() == AssetStatus.RETIRED) throw new IllegalStateException("Hurda demirbaş bakıma alınamaz.");

        AssetMaintenance m = new AssetMaintenance();
        m.setAssetId(assetId);
        m.setStartDate(LocalDateTime.now());
        m.setDescription(description);
        m.setNotes(notes);
        maintenanceRepository.save(m);
        asset.setStatus(AssetStatus.MAINTENANCE);
        assetRepository.save(asset);
    }

    @Transactional
    public void endMaintenance(int assetId, User requestingUser) {
        Asset asset = getOrThrow(assetId);
        checkWarehouseAccess(requestingUser, asset);
        maintenanceRepository.findByAssetIdAndEndDateIsNull(assetId)
                .orElseThrow(() -> new IllegalStateException("Aktif bakım kaydı bulunamadı."));
        maintenanceRepository.closeActiveByAssetId(assetId);
        asset.setStatus(AssetStatus.AVAILABLE);
        assetRepository.save(asset);
    }

    public List<AssetAssignment> getAssignmentHistory(int assetId) {
        return assignmentRepository.findByAssetIdOrderByAssignedDateDesc(assetId);
    }

    public List<AssetTransfer> getTransferHistory(int assetId) {
        return transferRepository.findByAssetIdOrderByTransferDateDesc(assetId);
    }

    public List<AssetMaintenance> getMaintenanceHistory(int assetId) {
        return maintenanceRepository.findByAssetIdOrderByStartDateDesc(assetId);
    }

    public List<Object[]> getWarehouseStatusSummary() {
        return assetRepository.getStatusSummaryByWarehouse();
    }

    public List<Object[]> getWarehouseStatusSummaryById(int warehouseId) {
        return assetRepository.getStatusSummaryByWarehouseId(warehouseId);
    }

    private void checkWarehouseAccess(User requestingUser, Asset asset) {
        if (isAdmin(requestingUser)) return;
        if (requestingUser.getWarehouse() == null || asset.getWarehouse() == null
                || requestingUser.getWarehouse().getId() != asset.getWarehouse().getId())
            throw new IllegalStateException("Bu depoya erişim yetkiniz yok.");
    }

    private boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }

    private Asset getOrThrow(int id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demirbaş bulunamadı: " + id));
    }
}
