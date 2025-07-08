package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "player_reward_history")
public class PlayerRewardHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "game_player_id", nullable = false)
    private GamePlayer player;

    private Integer milestone; // 1, 2, 3, 4, 5
    private Long minutesRequired;
    private Long rewardCoin;
    private LocalDateTime rewardedAt;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public GamePlayer getPlayer() { return player; }
    public void setPlayer(GamePlayer player) { this.player = player; }
    public Integer getMilestone() { return milestone; }
    public void setMilestone(Integer milestone) { this.milestone = milestone; }
    public Long getMinutesRequired() { return minutesRequired; }
    public void setMinutesRequired(Long minutesRequired) { this.minutesRequired = minutesRequired; }
    public Long getRewardCoin() { return rewardCoin; }
    public void setRewardCoin(Long rewardCoin) { this.rewardCoin = rewardCoin; }
    public LocalDateTime getRewardedAt() { return rewardedAt; }
    public void setRewardedAt(LocalDateTime rewardedAt) { this.rewardedAt = rewardedAt; }
} 