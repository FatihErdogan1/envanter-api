package org.example.inventoryapi.repository;

import org.example.inventoryapi.model.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Integer> {
    List<InventoryTransaction> findAllByOrderByTransactionDateDesc();
}
