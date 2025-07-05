package com.example.backend.controller;

import com.example.backend.entity.Game;
import com.example.backend.repository.GameRepository;
import com.example.backend.repository.GamePlayerRepository;
import com.example.backend.dto.GameResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = "http://localhost:3000")
public class GameController {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createGame(@RequestBody Game game) {
        if (gameRepository.findByName(game.getName()).isPresent()) {
            return ResponseEntity.badRequest().body("Game name already exists");
        }
        return ResponseEntity.ok(gameRepository.save(game));
    }

    @GetMapping
    public ResponseEntity<?> getAllGames() {
        List<Game> games = gameRepository.findAll();
        List<GameResponseDTO> result = games.stream().map(game -> new GameResponseDTO(
                game.getId(),
                game.getName(),
                game.getImageUrl(),
                game.getCategory(),
                game.getStatus(),
                gamePlayerRepository.countByGameId(game.getId()),
                game.getAvailableRoles(),
                game.getAvailableRanks()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateGame(@PathVariable Long id, @RequestBody Game updatedGame) {
        return gameRepository.findById(id)
            .map(game -> {
                game.setName(updatedGame.getName());
                game.setDescription(updatedGame.getDescription());
                game.setCategory(updatedGame.getCategory());
                game.setPlatform(updatedGame.getPlatform());
                game.setStatus(updatedGame.getStatus());
                game.setImageUrl(updatedGame.getImageUrl());
                game.setWebsiteUrl(updatedGame.getWebsiteUrl());
                game.setRequirements(updatedGame.getRequirements());
                game.setHasRoles(updatedGame.getHasRoles());
                game.setAvailableRoles(updatedGame.getAvailableRoles());
                game.setAvailableRanks(updatedGame.getAvailableRanks());
                return ResponseEntity.ok(gameRepository.save(game));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteGame(@PathVariable Long id) {
        if (!gameRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        long playerCount = gamePlayerRepository.countByGameId(id);
        if (playerCount > 0) {
            return ResponseEntity.badRequest().body("Không thể xóa game vì còn " + playerCount + " player đang đăng ký game này.");
        }
        gameRepository.deleteById(id);
        return ResponseEntity.ok("Game deleted successfully");
    }
} 