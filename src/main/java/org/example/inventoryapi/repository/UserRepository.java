package org.example.inventoryapi.repository;

import org.example.inventoryapi.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, int id);
    boolean existsByUsername(String username);
    boolean existsByUsernameAndIdNot(String username, int id);
}
