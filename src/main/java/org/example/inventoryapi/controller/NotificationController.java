package org.example.inventoryapi.controller;

import org.example.inventoryapi.model.entity.Notification;
import org.example.inventoryapi.model.entity.User;
import org.example.inventoryapi.repository.UserRepository;
import org.example.inventoryapi.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Notification> getNotifications(@AuthenticationPrincipal String username) {
        User user = getUser(username);
        log.debug("[Notification] GET /notifications: username={}, userId={}", username, user.getId());
        return notificationService.getNotifications(user.getId());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount(@AuthenticationPrincipal String username) {
        return Map.of("count", notificationService.getUnreadCount(getUser(username).getId()));
    }

    @PutMapping("/read-all")
    public void markAllAsRead(@AuthenticationPrincipal String username) {
        notificationService.markAllAsRead(getUser(username).getId());
    }

    @PutMapping("/{id}/read")
    public void markAsRead(@AuthenticationPrincipal String username, @PathVariable int id) {
        notificationService.markAsRead(id, getUser(username).getId());
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
    }
}
