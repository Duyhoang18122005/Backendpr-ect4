package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "moments")
@Data
public class Moment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "game_player_id", nullable = false)
    private GamePlayer gamePlayer;

    @Column(length = 1000)
    private String content; // Nội dung text của khoảnh khắc

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, HIDDEN, DELETED

    @OneToMany(mappedBy = "moment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MomentImage> images;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 