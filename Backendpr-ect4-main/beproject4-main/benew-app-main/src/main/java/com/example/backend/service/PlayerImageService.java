package com.example.backend.service;

import com.example.backend.entity.PlayerImage;
import com.example.backend.entity.GamePlayer;
import com.example.backend.repository.PlayerImageRepository;
import com.example.backend.repository.GamePlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlayerImageService {
    private final PlayerImageRepository playerImageRepository;
    private final GamePlayerRepository gamePlayerRepository;

    public PlayerImageService(PlayerImageRepository playerImageRepository, GamePlayerRepository gamePlayerRepository) {
        this.playerImageRepository = playerImageRepository;
        this.gamePlayerRepository = gamePlayerRepository;
    }

    public List<PlayerImage> getImagesByPlayerId(Long playerId) {
        return playerImageRepository.findByGamePlayerId(playerId);
    }

    public PlayerImage addImageToPlayer(Long playerId, String imageUrl) {
        GamePlayer gamePlayer = gamePlayerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Game player not found"));
        PlayerImage playerImage = new PlayerImage();
        playerImage.setGamePlayer(gamePlayer);
        playerImage.setImageUrl(imageUrl);
        return playerImageRepository.save(playerImage);
    }

    public void deleteImage(Long imageId) {
        playerImageRepository.deleteById(imageId);
    }

    public PlayerImage getImageById(Long imageId) {
        return playerImageRepository.findById(imageId).orElse(null);
    }
} 