package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reports")
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne
    @JoinColumn(name = "reported_player_id", nullable = false)
    private GamePlayer reportedPlayer;

    @Column(nullable = false)
    private String reason;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String status; // PENDING, INVESTIGATING, RESOLVED, REJECTED

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime resolvedAt;

    @Column
    private String resolution;
} 