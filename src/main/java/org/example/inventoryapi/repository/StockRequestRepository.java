package org.example.inventoryapi.repository;

import org.example.inventoryapi.model.entity.StockRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockRequestRepository extends JpaRepository<StockRequest, Integer> {
    List<StockRequest> findAllByOrderByRequestDateDesc();
    List<StockRequest> findByRequestedByUsernameOrderByRequestDateDesc(String username);
}
