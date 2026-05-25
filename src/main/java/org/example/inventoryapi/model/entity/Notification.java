package org.example.inventoryapi.model.entity;

import jakarta.persistence.*;
import org.example.inventoryapi.model.enums.NotificationType;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 255)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(name = "reference_id")
    private Integer referenceId;

    @Column(name = "is_read")
    private boolean read = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Notification() {}

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }
    public int getUserId()                          { return userId; }
    public void setUserId(int userId)               { this.userId = userId; }
    public String getTitle()                        { return title; }
    public void setTitle(String title)              { this.title = title; }
    public String getMessage()                      { return message; }
    public void setMessage(String message)          { this.message = message; }
    public NotificationType getType()               { return type; }
    public void setType(NotificationType type)      { this.type = type; }
    public Integer getReferenceId()                 { return referenceId; }
    public void setReferenceId(Integer referenceId) { this.referenceId = referenceId; }
    public boolean isRead()                         { return read; }
    public void setRead(boolean read)               { this.read = read; }
    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
