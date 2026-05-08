package org.example.inventoryapi.repository;

import org.example.inventoryapi.model.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, Integer> {
    boolean existsByNameIgnoreCase(String name);
}
