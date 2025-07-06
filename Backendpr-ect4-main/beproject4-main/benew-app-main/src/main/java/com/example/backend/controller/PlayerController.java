package com.example.backend.controller;

import com.example.backend.dto.PlayerStatsDTO;
import com.example.backend.entity.*;
import com.example.backend.repository.*;
import com.example.backend.service.PlayerStatsService;
import com.example.backend.service.UserService;
import com.example.backend.service.GamePlayerService;
import com.example.backend.service.NotificationService;
import com.example.backend.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/players")
@CrossOrigin(origins = "http://localhost:3000")
public class PlayerController {
    private final UserService userService;
    private final PlayerStatsService playerStatsService;
    private final PlayerFollowRepository playerFollowRepository;
    private final UserBlockRepository userBlockRepository;
    private final NotificationRepository notificationRepository;
    private final PlayerReviewRepository playerReviewRepository;
    private final GamePlayerService gamePlayerService;
    private final PaymentRepository paymentRepository;
    private final GameRepository gameRepository;
    private final NotificationService notificationService;
    private final OrderRepository orderRepository;

    private static final int MAX_FOLLOWING = 1000; // Giới hạn số người theo dõi

    public PlayerController(UserService userService,
                          PlayerStatsService playerStatsService,
                          PlayerFollowRepository playerFollowRepository,
                          UserBlockRepository userBlockRepository,
                          NotificationRepository notificationRepository,
                          PlayerReviewRepository playerReviewRepository,
                          GamePlayerService gamePlayerService,
                          PaymentRepository paymentRepository,
                          GameRepository gameRepository,
                          NotificationService notificationService,
                          OrderRepository orderRepository) {
        this.userService = userService;
        this.playerStatsService = playerStatsService;
        this.playerFollowRepository = playerFollowRepository;
        this.userBlockRepository = userBlockRepository;
        this.notificationRepository = notificationRepository;
        this.playerReviewRepository = playerReviewRepository;
        this.gamePlayerService = gamePlayerService;
        this.paymentRepository = paymentRepository;
        this.gameRepository = gameRepository;
        this.notificationService = notificationService;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/{playerId}/stats")
    @Operation(summary = "Get player statistics")
    public ResponseEntity<PlayerStatsDTO> getPlayerStats(@PathVariable Long playerId) {
        try {
            PlayerStatsDTO stats = playerStatsService.getPlayerStats(playerId);
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{gamePlayerId}/follow")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    @Operation(summary = "Follow a game player")
    public ResponseEntity<?> followPlayer(@PathVariable Long gamePlayerId, Authentication authentication) {
        User follower = userService.findByUsername(authentication.getName());
        GamePlayer gamePlayer = gamePlayerService.findById(gamePlayerId);

        // Kiểm tra chặn
        if (userBlockRepository.isBlocked(follower.getId(), gamePlayer.getUser().getId())) {
            return ResponseEntity.badRequest().body("Không thể theo dõi người chơi này");
        }

        if (follower.getId().equals(gamePlayer.getUser().getId())) {
            return ResponseEntity.badRequest().body("Không thể tự theo dõi chính mình");
        }

        if (playerFollowRepository.existsByFollowerIdAndGamePlayerId(follower.getId(), gamePlayerId)) {
            return ResponseEntity.badRequest().body("Đã theo dõi người chơi này");
        }

        // Kiểm tra giới hạn số người theo dõi
        Long followingCount = playerFollowRepository.countFollowingByFollowerId(follower.getId());
        if (followingCount >= MAX_FOLLOWING) {
            return ResponseEntity.badRequest().body("Đã đạt giới hạn số người theo dõi (" + MAX_FOLLOWING + ")");
        }

        PlayerFollow follow = new PlayerFollow();
        follow.setFollower(follower);
        follow.setGamePlayer(gamePlayer);
        playerFollowRepository.save(follow);

        // Gửi notification cho user được follow
        User followedUser = gamePlayer.getUser();
        notificationService.createNotification(
            followedUser.getId(),
            "Bạn có người theo dõi mới!",
            follower.getUsername() + " vừa theo dõi bạn.",
            "follow",
            null,
            null
        );

        return ResponseEntity.ok("Theo dõi thành công");
    }

    @DeleteMapping("/{gamePlayerId}/unfollow")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    @Operation(summary = "Unfollow a game player")
    public ResponseEntity<?> unfollowPlayer(@PathVariable Long gamePlayerId, Authentication authentication) {
        User follower = userService.findByUsername(authentication.getName());

        if (!playerFollowRepository.existsByFollowerIdAndGamePlayerId(follower.getId(), gamePlayerId)) {
            return ResponseEntity.badRequest().body("Chưa theo dõi người chơi này");
        }

        playerFollowRepository.deleteByFollowerIdAndGamePlayerId(follower.getId(), gamePlayerId);
        return ResponseEntity.ok("Bỏ theo dõi thành công");
    }

    @PostMapping("/{playerId}/block")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    @Operation(summary = "Block a player")
    public ResponseEntity<?> blockPlayer(@PathVariable Long playerId, Authentication authentication) {
        User blocker = userService.findByUsername(authentication.getName());
        User blocked = userService.findById(playerId);

        if (blocker.getId().equals(playerId)) {
            return ResponseEntity.badRequest().body("Không thể chặn chính mình");
        }

        if (userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), playerId)) {
            return ResponseEntity.badRequest().body("Đã chặn người chơi này");
        }

        // Tạo bản ghi chặn
        UserBlock block = new UserBlock();
        block.setBlocker(blocker);
        block.setBlocked(blocked);
        userBlockRepository.save(block);

        // Hủy theo dõi nếu đang theo dõi
        if (playerFollowRepository.existsByFollowerIdAndGamePlayerId(blocker.getId(), playerId)) {
            playerFollowRepository.deleteByFollowerIdAndGamePlayerId(blocker.getId(), playerId);
        }
        if (playerFollowRepository.existsByFollowerIdAndGamePlayerId(playerId, blocker.getId())) {
            playerFollowRepository.deleteByFollowerIdAndGamePlayerId(playerId, blocker.getId());
        }

        return ResponseEntity.ok("Chặn người chơi thành công");
    }

    @DeleteMapping("/{playerId}/unblock")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    @Operation(summary = "Unblock a player")
    public ResponseEntity<?> unblockPlayer(@PathVariable Long playerId, Authentication authentication) {
        User blocker = userService.findByUsername(authentication.getName());

        if (!userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), playerId)) {
            return ResponseEntity.badRequest().body("Chưa chặn người chơi này");
        }

        userBlockRepository.deleteByBlockerIdAndBlockedId(blocker.getId(), playerId);
        return ResponseEntity.ok("Bỏ chặn người chơi thành công");
    }

    @GetMapping("/popular")
    @Operation(summary = "Get popular players")
    public ResponseEntity<?> getPopularPlayers(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        
        List<Object[]> popularPlayers = playerFollowRepository.findPopularPlayers(limit, offset);
        
        return ResponseEntity.ok(popularPlayers.stream()
            .map(p -> Map.of(
                "playerId", p[0],
                "username", p[1],
                "followerCount", p[2],
                "averageRating", p[3]
            ))
            .collect(Collectors.toList()));
    }

    @GetMapping("/suggestions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get player suggestions")
    public ResponseEntity<?> getPlayerSuggestions(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        
        List<Object[]> suggestions = playerFollowRepository.findSuggestedPlayers(user.getId());
        
        return ResponseEntity.ok(suggestions.stream()
            .map(s -> Map.of(
                "playerId", s[0],
                "username", s[1],
                "followerCount", s[2],
                "averageRating", s[3],
                "commonFollowers", s[4]
            ))
            .collect(Collectors.toList()));
    }

    @GetMapping("/{gamePlayerId}/followers")
    @Operation(summary = "Get game player followers")
    public ResponseEntity<?> getPlayerFollowers(@PathVariable Long gamePlayerId) {
        List<PlayerFollow> follows = playerFollowRepository.findByGamePlayerId(gamePlayerId);
        Long followerCount = playerFollowRepository.countFollowersByGamePlayerId(gamePlayerId);
        
        return ResponseEntity.ok(Map.of(
            "followers", follows.stream()
                .map(f -> Map.of(
                    "id", f.getFollower().getId(),
                    "username", f.getFollower().getUsername(),
                    "followedAt", f.getCreatedAt()
                ))
                .collect(Collectors.toList()),
            "totalFollowers", followerCount
        ));
    }

    @GetMapping("/{gamePlayerId}/following")
    @Operation(summary = "Get players being followed")
    public ResponseEntity<?> getPlayerFollowing(@PathVariable Long gamePlayerId) {
        List<PlayerFollow> follows = playerFollowRepository.findByFollowerId(gamePlayerId);
        Long followingCount = playerFollowRepository.countFollowingByFollowerId(gamePlayerId);
        
        return ResponseEntity.ok(Map.of(
            "following", follows.stream()
                .map(f -> Map.of(
                    "id", f.getGamePlayer().getId(),
                    "username", f.getGamePlayer().getUsername(),
                    "game", f.getGamePlayer().getGame().getName(),
                    "followedAt", f.getCreatedAt()
                ))
                .collect(Collectors.toList()),
            "totalFollowing", followingCount
        ));
    }

    @GetMapping("/{gamePlayerId}/is-following")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Check if following a game player")
    public ResponseEntity<?> checkFollowingStatus(@PathVariable Long gamePlayerId, Authentication authentication) {
        User follower = userService.findByUsername(authentication.getName());
        boolean isFollowing = playerFollowRepository.existsByFollowerIdAndGamePlayerId(follower.getId(), gamePlayerId);
        return ResponseEntity.ok(Map.of("isFollowing", isFollowing));
    }

    @GetMapping("/notifications")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get user notifications")
    public ResponseEntity<?> getNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        
        User user = userService.findByUsername(authentication.getName());
        List<Notification> notifications = unreadOnly ?
            notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId()) :
            notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            
        Long unreadCount = notificationRepository.countUnreadNotifications(user.getId());
        
        return ResponseEntity.ok(Map.of(
            "notifications", notifications,
            "unreadCount", unreadCount
        ));
    }

    @PostMapping("/notifications/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<?> markNotificationAsRead(
            @PathVariable Long notificationId,
            Authentication authentication) {
        
        User user = userService.findByUsername(authentication.getName());
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found"));
            
        if (!notification.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Không có quyền truy cập thông báo này");
        }
        
        notification.setRead(true);
        notificationRepository.save(notification);
        
        return ResponseEntity.ok("Đã đánh dấu đã đọc");
    }

    @GetMapping
    @Operation(summary = "Get all players")
    public ResponseEntity<List<GamePlayer>> getAllPlayers() {
        return ResponseEntity.ok(gamePlayerService.findAll());
    }

    @GetMapping("/available")
    @Operation(summary = "Get available game players")
    public ResponseEntity<List<GamePlayer>> getAvailablePlayers() {
        return ResponseEntity.ok(gamePlayerService.findByStatus("AVAILABLE"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get game player by ID")
    public ResponseEntity<GamePlayer> getPlayer(@PathVariable Long id) {
        return ResponseEntity.ok(gamePlayerService.findById(id));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Get status of game player by ID")
    public ResponseEntity<?> getPlayerStatus(@PathVariable Long id) {
        try {
            GamePlayer player = gamePlayerService.findById(id);
            if (player == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(Map.of(
                "id", player.getId(),
                "status", player.getStatus()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace(); // Log lỗi chi tiết
            return ResponseEntity.status(500).body("Internal server error: " + e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new game player")
    public ResponseEntity<GamePlayer> createPlayer(@Valid @RequestBody GamePlayerRequest request) {
        GamePlayer gamePlayer = new GamePlayer();
        updateGamePlayerFromRequest(gamePlayer, request);
        return ResponseEntity.ok(gamePlayerService.save(gamePlayer));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update game player information")
    public ResponseEntity<GamePlayer> updatePlayer(
            @PathVariable Long id,
            @Valid @RequestBody GamePlayerRequest request) {
        GamePlayer gamePlayer = gamePlayerService.findById(id);
        updateGamePlayerFromRequest(gamePlayer, request);
        return ResponseEntity.ok(gamePlayerService.save(gamePlayer));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a game player")
    public ResponseEntity<?> deletePlayer(@PathVariable Long id) {
        gamePlayerService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/hire")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Hire a game player")
    public ResponseEntity<GamePlayer> hirePlayer(@PathVariable Long id, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        return ResponseEntity.ok(gamePlayerService.hirePlayer(id, user));
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Return a game player")
    public ResponseEntity<GamePlayer> returnPlayer(@PathVariable Long id) {
        return ResponseEntity.ok(gamePlayerService.returnPlayer(id));
    }

    @GetMapping("/{gamePlayerId}/followers/count")
    @Operation(summary = "Get follower count of a game player")
    public ResponseEntity<?> getPlayerFollowerCount(@PathVariable Long gamePlayerId) {
        Long followerCount = playerFollowRepository.countFollowersByGamePlayerId(gamePlayerId);
        return ResponseEntity.ok(Map.of(
            "gamePlayerId", gamePlayerId,
            "followerCount", followerCount
        ));
    }

    @GetMapping("/{gamePlayerId}/hire-hours")
    @Operation(summary = "Get total hire hours of a game player")
    public ResponseEntity<?> getPlayerTotalHireHours(@PathVariable Long gamePlayerId) {
        List<Order> orders = orderRepository.findByPlayer_IdAndStatus(gamePlayerId, "COMPLETED");
        int totalHours = orders.stream()
            .filter(o -> o.getStartTime() != null && o.getEndTime() != null)
            .mapToInt(o -> (int) java.time.Duration.between(o.getStartTime(), o.getEndTime()).toHours())
            .sum();
        return ResponseEntity.ok(Map.of(
            "gamePlayerId", gamePlayerId,
            "totalHireHours", totalHours
        ));
    }

    private void updateGamePlayerFromRequest(GamePlayer gamePlayer, GamePlayerRequest request) {
        User user = userService.findById(request.getUserId());
        Game game = gameRepository.findById(request.getGameId())
            .orElseThrow(() -> new ResourceNotFoundException("Game not found"));

        gamePlayer.setUser(user);
        gamePlayer.setGame(game);
        gamePlayer.setUsername(request.getUsername());
        gamePlayer.setRank(request.getRank());
        gamePlayer.setRole(request.getRole());
        gamePlayer.setServer(request.getServer());
        gamePlayer.setPricePerHour(request.getPricePerHour());
        gamePlayer.setDescription(request.getDescription());
        gamePlayer.setStatus("AVAILABLE");
    }

    @Data
    public static class GamePlayerRequest {
        @NotNull(message = "User ID is required")
        private Long userId;

        @NotNull(message = "Game ID is required")
        private Long gameId;

        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Rank is required")
        private String rank;

        private String role;

        @NotBlank(message = "Server is required")
        private String server;

        @NotNull(message = "Price per hour is required")
        @DecimalMin(value = "0.0", message = "Price must be greater than 0")
        private BigDecimal pricePerHour;

        @Size(max = 500, message = "Description must be less than 500 characters")
        private String description;
    }
} 