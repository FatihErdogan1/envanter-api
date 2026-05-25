package org.example.inventoryapi.repository;

import org.example.inventoryapi.model.entity.Notification;
import org.example.inventoryapi.model.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(int userId);

    long countByUserIdAndReadFalse(int userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId AND n.read = false")
    void markAllAsRead(@Param("userId") int userId);

    boolean existsByTypeAndReferenceIdAndCreatedAtAfter(NotificationType type, Integer referenceId, LocalDateTime after);
}
