package com.example.backend.repository;

import com.example.backend.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findByName(String name);
    List<Game> findByCategory(String category);
    List<Game> findByPlatform(String platform);
    List<Game> findByStatus(String status);
    List<Game> findByCategoryAndPlatform(String category, String platform);
} 