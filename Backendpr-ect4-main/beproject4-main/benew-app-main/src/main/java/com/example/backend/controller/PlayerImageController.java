package com.example.backend.controller;

import com.example.backend.entity.PlayerImage;
import com.example.backend.service.PlayerImageService;
import com.example.backend.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/game-players/{playerId}/images")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Player Image", description = "Player image gallery management APIs")
public class PlayerImageController {
    private final PlayerImageService playerImageService;
    private static final Logger log = LoggerFactory.getLogger(PlayerImageController.class);

    @Value("${player.image.upload-dir:uploads/player-images}")
    private String uploadDir;

    public PlayerImageController(PlayerImageService playerImageService) {
        this.playerImageService = playerImageService;
    }

    @Operation(summary = "Lấy tất cả ảnh của player")
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getPlayerImages(@Parameter(description = "Player ID") @PathVariable Long playerId) {
        try {
            List<PlayerImage> images = playerImageService.getImagesByPlayerId(playerId);
            
            List<Map<String, Object>> imageList = images.stream().map(image -> {
                Map<String, Object> imageData = new HashMap<>();
                imageData.put("id", image.getId());
                imageData.put("imageUrl", image.getImageUrl());
                imageData.put("fileName", extractFileNameFromUrl(image.getImageUrl()));
                imageData.put("uploadTime", extractTimeFromFileName(image.getImageUrl()));
                return imageData;
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("images", imageList);
            response.put("totalImages", images.size());
            response.put("playerId", playerId);

            return ResponseEntity.ok(new ApiResponse<>(true, "Lấy kho ảnh player thành công", response));
        } catch (Exception e) {
            log.error("Error getting player images: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Lỗi khi lấy kho ảnh: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Lấy danh sách URL ảnh của player")
    @GetMapping("/urls")
    public ResponseEntity<ApiResponse<?>> getPlayerImageUrls(@Parameter(description = "Player ID") @PathVariable Long playerId) {
        try {
        List<String> imageUrls = playerImageService.getImagesByPlayerId(playerId)
                .stream().map(PlayerImage::getImageUrl).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("imageUrls", imageUrls);
            response.put("totalImages", imageUrls.size());
            response.put("playerId", playerId);

            return ResponseEntity.ok(new ApiResponse<>(true, "Lấy danh sách URL ảnh thành công", response));
        } catch (Exception e) {
            log.error("Error getting player image URLs: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Lỗi khi lấy danh sách URL ảnh: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Upload ảnh vào kho ảnh của player")
    @PostMapping
    public ResponseEntity<ApiResponse<?>> uploadPlayerImage(
            @Parameter(description = "Player ID") @PathVariable Long playerId, 
            @RequestParam("file") MultipartFile file) {
        log.info("[UPLOAD] playerId={}, originalFilename={}", playerId, file.getOriginalFilename());
        try {
            // Kiểm tra file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "File không được để trống", null));
            }
            // Bỏ kiểm tra định dạng file, cho phép upload mọi loại file
            // Lấy đường dẫn tuyệt đối tới thư mục gốc project
            String rootPath = new File("").getAbsolutePath();
            File dir = new File(rootPath, uploadDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                log.info("[UPLOAD] Created upload dir: {} => {}", dir.getAbsolutePath(), created);
            }
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            File dest = new File(dir, fileName);
            dest.getParentFile().mkdirs();
            log.info("[UPLOAD] Saving file to: {}", dest.getAbsolutePath());
            file.transferTo(dest);
            String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/" + uploadDir + "/")
                    .path(fileName)
                    .toUriString();
            log.info("[UPLOAD] File URL: {}", fileUrl);
            PlayerImage savedImage = playerImageService.addImageToPlayer(playerId, fileUrl);
            Map<String, Object> response = new HashMap<>();
            response.put("imageId", savedImage.getId());
            response.put("imageUrl", savedImage.getImageUrl());
            response.put("fileName", extractFileNameFromUrl(savedImage.getImageUrl()));
            response.put("uploadTime", extractTimeFromFileName(savedImage.getImageUrl()));
            response.put("playerId", playerId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Upload ảnh thành công", response));
        } catch (Exception e) {
            log.error("[UPLOAD][ERROR] playerId={}, file={}, error={}", playerId, file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(new ApiResponse<>(false, "Upload thất bại: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Xóa ảnh khỏi kho ảnh của player")
    @DeleteMapping("/{imageId}")
    public ResponseEntity<ApiResponse<?>> deletePlayerImage(
            @Parameter(description = "Player ID") @PathVariable Long playerId, 
            @Parameter(description = "Image ID") @PathVariable Long imageId) {
        try {
        playerImageService.deleteImage(imageId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Xóa ảnh thành công", null));
        } catch (Exception e) {
            log.error("Error deleting player image: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Lỗi khi xóa ảnh: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Lấy thông tin chi tiết của một ảnh")
    @GetMapping("/{imageId}")
    public ResponseEntity<ApiResponse<?>> getPlayerImage(
            @Parameter(description = "Player ID") @PathVariable Long playerId, 
            @Parameter(description = "Image ID") @PathVariable Long imageId) {
        try {
            PlayerImage image = playerImageService.getImageById(imageId);
            if (image == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> imageData = new HashMap<>();
            imageData.put("id", image.getId());
            imageData.put("imageUrl", image.getImageUrl());
            imageData.put("fileName", extractFileNameFromUrl(image.getImageUrl()));
            imageData.put("uploadTime", extractTimeFromFileName(image.getImageUrl()));
            imageData.put("playerId", playerId);

            return ResponseEntity.ok(new ApiResponse<>(true, "Lấy thông tin ảnh thành công", imageData));
        } catch (Exception e) {
            log.error("Error getting player image: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Lỗi khi lấy thông tin ảnh: " + e.getMessage(), null));
        }
    }

    // Helper methods
    private String extractFileNameFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return "";
        }
        String[] parts = imageUrl.split("/");
        if (parts.length > 0) {
            String fileName = parts[parts.length - 1];
            // Loại bỏ timestamp ở đầu
            if (fileName.contains("_")) {
                return fileName.substring(fileName.indexOf("_") + 1);
            }
            return fileName;
        }
        return "";
    }

    private String extractTimeFromFileName(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return "";
        }
        String[] parts = imageUrl.split("/");
        if (parts.length > 0) {
            String fileName = parts[parts.length - 1];
            // Lấy timestamp ở đầu
            if (fileName.contains("_")) {
                String timestamp = fileName.substring(0, fileName.indexOf("_"));
                try {
                    long time = Long.parseLong(timestamp);
                    return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                        .format(new java.util.Date(time));
                } catch (NumberFormatException e) {
                    return "";
                }
            }
        }
        return "";
    }
} 