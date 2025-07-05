package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "skills")
public class Skill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "game_player_id", nullable = false)
    private GamePlayer gamePlayer;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String level; // BEGINNER, INTERMEDIATE, ADVANCED, EXPERT

    @Column
    private String description;

    @Column
    private String certification;

    @Column
    private String proof; // URL to proof of skill
} 