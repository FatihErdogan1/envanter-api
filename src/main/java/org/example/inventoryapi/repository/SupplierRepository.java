package org.example.inventoryapi.repository;

import org.example.inventoryapi.model.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Integer> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByContactEmailIgnoreCaseAndIdNot(String email, int id);
    boolean existsByContactEmailIgnoreCase(String email);
    boolean existsByPhone(String phone);
    boolean existsByPhoneAndIdNot(String phone, int id);
}
