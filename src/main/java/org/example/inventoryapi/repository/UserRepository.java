package org.example.inventoryapi.repository;

import org.example.inventoryapi.model.entity.User;
import org.example.inventoryapi.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, int id);
    boolean existsByUsername(String username);
    boolean existsByUsernameAndIdNot(String username, int id);

    List<User> findByRole(Role role);
    List<User> findByRoleAndWarehouse_Id(Role role, int warehouseId);
    List<User> findByRoleAndSupplier_Id(Role role, int supplierId);
}
