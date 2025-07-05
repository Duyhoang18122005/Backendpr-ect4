package com.example.backend.service;

import com.example.backend.entity.GamePlayer;
import com.example.backend.entity.User;
import com.example.backend.repository.GamePlayerRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Validated
public class GamePlayerServiceImpl {

    private final GamePlayerRepository gamePlayerRepository;

    public GamePlayerServiceImpl(GamePlayerRepository gamePlayerRepository) {
        this.gamePlayerRepository = gamePlayerRepository;
    }

    public List<GamePlayer> findAll() {
        return gamePlayerRepository.findAll();
    }

    public List<GamePlayer> findByStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
        return gamePlayerRepository.findByStatus(status);
    }

    public List<GamePlayer> findByGameId(Long gameId) {
        if (gameId == null) {
            throw new IllegalArgumentException("Game ID cannot be null");
        }
        return gamePlayerRepository.findByGameId(gameId);
    }

    public List<GamePlayer> findByRank(String rank) {
        if (rank == null || rank.trim().isEmpty()) {
            throw new IllegalArgumentException("Rank cannot be null or empty");
        }
        return gamePlayerRepository.findByRank(rank);
    }

    public List<GamePlayer> findByRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }
        return gamePlayerRepository.findByRole(role);
    }

    public List<GamePlayer> findByServer(String server) {
        if (server == null || server.trim().isEmpty()) {
            throw new IllegalArgumentException("Server cannot be null or empty");
        }
        return gamePlayerRepository.findByServer(server);
    }

    public List<GamePlayer> findByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return gamePlayerRepository.findByUserId(userId);
    }

    public List<GamePlayer> findByHiredById(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return gamePlayerRepository.findByHiredById(userId);
    }

    public GamePlayer findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        return gamePlayerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Game player not found with id: " + id));
    }

    @Transactional
    public GamePlayer save(GamePlayer gamePlayer) {
        if (gamePlayer == null) {
            throw new IllegalArgumentException("Game player cannot be null");
        }
        return gamePlayerRepository.save(gamePlayer);
    }

    @Transactional
    public void deleteById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        if (!gamePlayerRepository.existsById(id)) {
            throw new RuntimeException("Game player not found with id: " + id);
        }
        gamePlayerRepository.deleteById(id);
    }

    @Transactional
    public GamePlayer hirePlayer(Long id, User user, Integer hours) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (hours == null || hours < 1) {
            throw new IllegalArgumentException("Hours must be at least 1");
        }

        GamePlayer gamePlayer = findById(id);
        if (!"AVAILABLE".equals(gamePlayer.getStatus())) {
            throw new RuntimeException("Game player is not available for hire");
        }

        gamePlayer.setStatus("HIRED");
        gamePlayer.setHiredBy(user);
        gamePlayer.setHireDate(LocalDateTime.now());
        gamePlayer.setHoursHired(hours);
        gamePlayer.setReturnDate(LocalDateTime.now().plusDays(1));

        return gamePlayerRepository.save(gamePlayer);
    }

    @Transactional
    public GamePlayer returnPlayer(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }

        GamePlayer gamePlayer = findById(id);
        if (!"HIRED".equals(gamePlayer.getStatus())) {
            throw new RuntimeException("Game player is not currently hired");
        }

        gamePlayer.setStatus("AVAILABLE");
        gamePlayer.setHiredBy(null);
        gamePlayer.setHireDate(null);
        gamePlayer.setReturnDate(null);
        gamePlayer.setHoursHired(null);

        return gamePlayerRepository.save(gamePlayer);
    }

    @Transactional
    public GamePlayer updateRating(Long id, Double rating) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        if (rating == null) {
            throw new IllegalArgumentException("Rating cannot be null");
        }
        if (rating < 0 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 0 and 5");
        }

        GamePlayer gamePlayer = findById(id);
        if (gamePlayer.getRating() == null) {
            gamePlayer.setRating(rating);
        } else {
            gamePlayer.setRating((gamePlayer.getRating() + rating) / 2);
        }

        return gamePlayerRepository.save(gamePlayer);
    }
}
