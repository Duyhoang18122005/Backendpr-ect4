package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String type; // FOLLOW, BLOCK, etc.

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private boolean isRead = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // New fields
    @Column(nullable = false)
    private String title = "";
    private String actionUrl;
    private String status;
    private Long orderId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (title == null) title = "";
    }
} 