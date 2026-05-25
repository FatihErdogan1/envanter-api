package org.example.inventoryapi.repository;

import jakarta.persistence.LockModeType;
import org.example.inventoryapi.model.entity.SupplierOrder;
import org.example.inventoryapi.model.enums.SupplierOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupplierOrderRepository extends JpaRepository<SupplierOrder, Integer> {
    List<SupplierOrder> findAllByOrderByOrderDateDesc();
    List<SupplierOrder> findBySupplier_IdOrderByOrderDateDesc(int supplierId);
    List<SupplierOrder> findByStatusOrderByOrderDateDesc(SupplierOrderStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM SupplierOrder o WHERE o.id = :id")
    Optional<SupplierOrder> findByIdWithLock(@Param("id") int id);
}
