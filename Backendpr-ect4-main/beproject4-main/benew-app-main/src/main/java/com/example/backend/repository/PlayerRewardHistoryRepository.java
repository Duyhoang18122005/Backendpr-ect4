package com.example.backend.repository;

import com.example.backend.entity.PlayerRewardHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlayerRewardHistoryRepository extends JpaRepository<PlayerRewardHistory, Long> {
    List<PlayerRewardHistory> findByPlayerId(Long playerId);
} 