package com.example.backend.repository;

import com.example.backend.entity.GamePlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {
    List<GamePlayer> findByGameId(Long gameId);
    List<GamePlayer> findByUserId(Long userId);
    List<GamePlayer> findByStatus(String status);
    List<GamePlayer> findByRank(String rank);
    List<GamePlayer> findByRole(String role);
    List<GamePlayer> findByServer(String server);
    List<GamePlayer> findByHiredById(Long userId);
    long countByGameId(Long gameId);
}