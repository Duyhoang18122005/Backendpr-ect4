package com.example.backend.service;

import com.example.backend.dto.MomentDTO;
import com.example.backend.dto.CreateMomentRequest;
import com.example.backend.entity.*;
import com.example.backend.repository.*;
import com.example.backend.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MomentServiceImpl implements MomentService {

    @Autowired
    private MomentRepository momentRepository;

    @Autowired
    private MomentImageRepository momentImageRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private PlayerFollowRepository playerFollowRepository;

    @Autowired
    private NotificationService notificationService;

    @Override
    public MomentDTO createMoment(Long gamePlayerId, CreateMomentRequest request) {
        // Kiểm tra game player tồn tại
        GamePlayer gamePlayer = gamePlayerRepository.findById(gamePlayerId)
                .orElseThrow(() -> new ResourceNotFoundException("Game player not found"));

        // Kiểm tra validation
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be empty");
        }
        
        if (request.getContent().length() > 1000) {
            throw new IllegalArgumentException("Content cannot exceed 1000 characters");
        }

        // Tạo moment mới
        Moment moment = new Moment();
        moment.setGamePlayer(gamePlayer);
        moment.setContent(request.getContent().trim());
        moment.setStatus("ACTIVE");
        
        Moment savedMoment = momentRepository.save(moment);

        // Lưu ảnh nếu có
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            // Giới hạn tối đa 10 ảnh
            if (request.getImageUrls().size() > 10) {
                throw new IllegalArgumentException("Maximum 10 images allowed per moment");
            }
            
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                String imageUrl = request.getImageUrls().get(i);
                if (imageUrl == null || imageUrl.trim().isEmpty()) {
                    throw new IllegalArgumentException("Image URL cannot be empty");
                }
                
                MomentImage momentImage = new MomentImage();
                momentImage.setMoment(savedMoment);
                momentImage.setImageUrl(imageUrl.trim());
                momentImage.setDisplayOrder(i + 1);
                momentImageRepository.save(momentImage);
            }
        }

        // Gửi notification cho tất cả followers
        sendMomentNotificationToFollowers(gamePlayer, savedMoment);

        return convertToDTO(savedMoment, gamePlayer);
    }

    @Override
    public MomentDTO getMomentById(Long momentId) {
        Moment moment = momentRepository.findById(momentId)
                .orElseThrow(() -> new ResourceNotFoundException("Moment not found"));
        
        return convertToDTO(moment, moment.getGamePlayer());
    }

    @Override
    public Page<MomentDTO> getMomentsByGamePlayerId(Long gamePlayerId, Pageable pageable) {
        Page<Moment> moments = momentRepository.findByGamePlayerIdAndStatusOrderByCreatedAtDesc(
                gamePlayerId, "ACTIVE", pageable);
        
        return moments.map(moment -> convertToDTO(moment, moment.getGamePlayer()));
    }

    @Override
    public Page<MomentDTO> getMyMoments(Long userId, Pageable pageable) {
        Page<Moment> moments = momentRepository.findByUserIdAndStatus(userId, "ACTIVE", pageable);
        
        return moments.map(moment -> convertToDTO(moment, moment.getGamePlayer()));
    }

    @Override
    public Page<MomentDTO> getMomentFeed(Long userId, Pageable pageable) {
        // Lấy danh sách game player mà user đang follow
        List<PlayerFollow> follows = playerFollowRepository.findByFollowerId(userId);
        List<Long> followedPlayerIds = follows.stream()
                .map(follow -> follow.getGamePlayer().getId())
                .collect(Collectors.toList());

        if (followedPlayerIds.isEmpty()) {
            // Nếu không follow ai, trả về page rỗng
            return Page.empty(pageable);
        }

        // Lấy moment của những player được follow
        Page<Moment> moments = momentRepository.findByGamePlayerIdInAndStatusOrderByCreatedAtDesc(
                followedPlayerIds, "ACTIVE", pageable);
        
        return moments.map(moment -> convertToDTO(moment, moment.getGamePlayer()));
    }

    @Override
    public Page<MomentDTO> getAllMoments(Pageable pageable) {
        Page<Moment> moments = momentRepository.findByStatusOrderByCreatedAtDesc("ACTIVE", pageable);
        return moments.map(moment -> convertToDTO(moment, moment.getGamePlayer()));
    }

    @Override
    public MomentDTO updateMoment(Long momentId, Long userId, CreateMomentRequest request) {
        Moment moment = momentRepository.findByIdAndUserId(momentId, userId);
        if (moment == null) {
            throw new ResourceNotFoundException("Moment not found or access denied");
        }

        moment.setContent(request.getContent());
        Moment updatedMoment = momentRepository.save(moment);

        // Cập nhật ảnh
        momentImageRepository.deleteByMomentId(momentId);
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                MomentImage momentImage = new MomentImage();
                momentImage.setMoment(updatedMoment);
                momentImage.setImageUrl(request.getImageUrls().get(i));
                momentImage.setDisplayOrder(i + 1);
                momentImageRepository.save(momentImage);
            }
        }

        return convertToDTO(updatedMoment, updatedMoment.getGamePlayer());
    }

    @Override
    public void deleteMoment(Long momentId, Long userId) {
        Moment moment = momentRepository.findByIdAndUserId(momentId, userId);
        if (moment == null) {
            throw new ResourceNotFoundException("Moment not found or access denied");
        }

        moment.setStatus("DELETED");
        momentRepository.save(moment);
    }

    @Override
    public void toggleMomentVisibility(Long momentId, Long userId) {
        Moment moment = momentRepository.findByIdAndUserId(momentId, userId);
        if (moment == null) {
            throw new ResourceNotFoundException("Moment not found or access denied");
        }

        String newStatus = "ACTIVE".equals(moment.getStatus()) ? "HIDDEN" : "ACTIVE";
        moment.setStatus(newStatus);
        momentRepository.save(moment);
    }

    private void sendMomentNotificationToFollowers(GamePlayer gamePlayer, Moment moment) {
        // Lấy danh sách followers
        List<PlayerFollow> follows = playerFollowRepository.findByGamePlayerId(gamePlayer.getId());
        
        for (PlayerFollow follow : follows) {
            User follower = follow.getFollower();
            
            // Tạo notification
            String title = gamePlayer.getUsername() + " vừa đăng khoảnh khắc mới!";
            String message = moment.getContent() != null && !moment.getContent().isEmpty() 
                    ? moment.getContent().substring(0, Math.min(moment.getContent().length(), 50)) + "..."
                    : "Hãy xem khoảnh khắc mới của " + gamePlayer.getUsername() + " ngay nhé!";
            
            notificationService.createNotification(
                follower.getId(),
                title,
                message,
                "moment",
                "/player/" + gamePlayer.getId() + "/moments",
                moment.getId().toString()
            );
        }
    }

    private MomentDTO convertToDTO(Moment moment, GamePlayer gamePlayer) {
        MomentDTO dto = new MomentDTO();
        dto.setId(moment.getId());
        dto.setGamePlayerId(gamePlayer.getId());
        dto.setGamePlayerUsername(gamePlayer.getUsername());
        dto.setGameName(gamePlayer.getGame().getName());
        dto.setContent(moment.getContent());
        dto.setCreatedAt(moment.getCreatedAt());
        dto.setUpdatedAt(moment.getUpdatedAt());
        dto.setStatus(moment.getStatus());
        
        // Lấy ảnh
        List<MomentImage> images = momentImageRepository.findByMomentIdOrderByDisplayOrderAsc(moment.getId());
        List<String> imageUrls = images.stream()
                .map(MomentImage::getImageUrl)
                .collect(Collectors.toList());
        dto.setImageUrls(imageUrls);
        
        // Lấy số followers
        Long followerCount = playerFollowRepository.countFollowersByGamePlayerId(gamePlayer.getId());
        dto.setFollowerCount(followerCount);
        
        // Set playerUserId
        if (gamePlayer.getUser() != null) {
            dto.setPlayerUserId(gamePlayer.getUser().getId());
        } else {
            dto.setPlayerUserId(null);
        }
        
        return dto;
    }

    @Override
    public boolean isGamePlayerOwner(Long gamePlayerId, Long userId) {
        System.out.println("=== isGamePlayerOwner ===");
        System.out.println("gamePlayerId: " + gamePlayerId);
        System.out.println("userId: " + userId);
        
        return gamePlayerRepository.findById(gamePlayerId)
                .map(gamePlayer -> {
                    System.out.println("GamePlayer found: " + gamePlayer.getUsername());
                    System.out.println("GamePlayer user ID: " + gamePlayer.getUser().getId());
                    System.out.println("Comparing: " + gamePlayer.getUser().getId() + " == " + userId);
                    boolean isOwner = gamePlayer.getUser().getId().equals(userId);
                    System.out.println("Is owner: " + isOwner);
                    return isOwner;
                })
                .orElse(false);
    }
} 