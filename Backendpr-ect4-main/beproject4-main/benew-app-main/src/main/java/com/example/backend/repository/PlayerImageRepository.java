package com.example.backend.repository;

import com.example.backend.entity.PlayerImage;
import com.example.backend.entity.GamePlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlayerImageRepository extends JpaRepository<PlayerImage, Long> {
    List<PlayerImage> findByGamePlayer(GamePlayer gamePlayer);
    List<PlayerImage> findByGamePlayerId(Long gamePlayerId);
} 