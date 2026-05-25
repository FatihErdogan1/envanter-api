package org.example.inventoryapi.service;

import org.example.inventoryapi.model.entity.Notification;
import org.example.inventoryapi.model.enums.NotificationType;
import org.example.inventoryapi.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createNotification(int userId, String title, String message,
                                   NotificationType type, Integer referenceId) {
        try {
            log.info("[Bildirim] Oluşturuluyor: userId={}, type={}, title={}", userId, type, title);
            Notification n = new Notification();
            n.setUserId(userId);
            n.setTitle(title);
            n.setMessage(message);
            n.setType(type);
            n.setReferenceId(referenceId);
            n.setRead(false);
            n.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(n);
            log.info("[Bildirim] Kaydedildi: id={}, userId={}", n.getId(), userId);
        } catch (Exception e) {
            log.warn("[Bildirim] Oluşturulamadı: userId={}, title={}, hata={}", userId, title, e.getMessage(), e);
        }
    }

    public List<Notification> getNotifications(int userId) {
        return notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount(int userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAllAsRead(int userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public void markAsRead(int notificationId, int userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUserId() == userId) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    public boolean wasLowStockNotifiedRecently(int productId) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        return notificationRepository.existsByTypeAndReferenceIdAndCreatedAtAfter(
                NotificationType.LOW_STOCK, productId, cutoff);
    }
}
