package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "conversations")
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    @ManyToOne
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    @Column(nullable = false)
    private LocalDateTime lastMessageTime = LocalDateTime.now();

    @Column(length = 500)
    private String lastMessageContent;

    @Column(nullable = false)
    private int unreadCount = 0;

    @PrePersist
    protected void onCreate() {
        if (lastMessageTime == null) {
            lastMessageTime = LocalDateTime.now();
        }
    }
} 