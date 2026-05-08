package org.example.inventoryapi.service;

import org.example.inventoryapi.model.entity.Supplier;
import org.example.inventoryapi.repository.AssetRepository;
import org.example.inventoryapi.repository.SupplierRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class SupplierService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final SupplierRepository supplierRepository;
    private final AssetRepository    assetRepository;

    public SupplierService(SupplierRepository supplierRepository, AssetRepository assetRepository) {
        this.supplierRepository = supplierRepository;
        this.assetRepository    = assetRepository;
    }

    public List<Supplier> getAllSuppliers() { return supplierRepository.findAll(); }

    public Supplier addSupplier(Supplier supplier) {
        if (supplier.getName() == null || supplier.getName().isBlank())
            throw new IllegalArgumentException("Tedarikçi adı boş bırakılamaz.");
        if (supplierRepository.existsByNameIgnoreCase(supplier.getName()))
            throw new IllegalArgumentException("Bu isimde bir tedarikçi zaten sistemde kayıtlıdır.");
        validateEmailIfPresent(supplier.getContactEmail(), -1);
        if (supplier.getPhone() != null && supplier.getPhone().length() > 20)
            throw new IllegalArgumentException("Telefon numarası 20 karakterden uzun olamaz.");
        validatePhoneIfPresent(supplier.getPhone(), -1);
        return supplierRepository.save(supplier);
    }

    public Supplier updateSupplier(int id, Supplier updated) {
        Supplier existing = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tedarikçi bulunamadı."));
        validateEmailIfPresent(updated.getContactEmail(), id);
        validatePhoneIfPresent(updated.getPhone(), id);
        existing.setName(updated.getName());
        existing.setContactEmail(updated.getContactEmail());
        existing.setPhone(updated.getPhone());
        existing.setAddress(updated.getAddress());
        return supplierRepository.save(existing);
    }

    public void deleteSupplier(int id) {
        int count = assetRepository.countBySupplierId(id);
        if (count > 0)
            throw new IllegalStateException("Bu tedarikçiye ait " + count + " demirbaş var. Silinemez.");
        supplierRepository.deleteById(id);
    }

    private void validatePhoneIfPresent(String phone, int excludeId) {
        if (phone == null || phone.isBlank()) return;
        boolean duplicate = excludeId < 0
                ? supplierRepository.existsByPhone(phone)
                : supplierRepository.existsByPhoneAndIdNot(phone, excludeId);
        if (duplicate)
            throw new IllegalArgumentException("Bu telefon numarası zaten kayıtlı.");
    }

    private void validateEmailIfPresent(String email, int excludeId) {
        if (email == null || email.isBlank()) return;
        if (!EMAIL_PATTERN.matcher(email.trim()).matches())
            throw new IllegalArgumentException("Geçerli bir e-posta adresi giriniz.");
        boolean duplicate = excludeId < 0
                ? supplierRepository.existsByContactEmailIgnoreCase(email)
                : supplierRepository.existsByContactEmailIgnoreCaseAndIdNot(email, excludeId);
        if (duplicate)
            throw new IllegalArgumentException("Bu e-posta adresi başka bir tedarikçiye kayıtlıdır.");
    }
}
