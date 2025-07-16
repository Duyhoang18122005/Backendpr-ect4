package com.example.backend.controller;

import com.example.backend.dto.GamePlayerStatusResponse;
import com.example.backend.entity.GamePlayer;
import com.example.backend.service.GamePlayerService;
import com.example.backend.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Arrays;
import com.example.backend.entity.Game;
import com.example.backend.repository.GameRepository;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.service.UserService;
import com.example.backend.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import com.example.backend.entity.Payment;
import com.example.backend.repository.PaymentRepository;
import com.example.backend.service.NotificationService;
import org.springframework.security.core.Authentication;
import java.util.stream.Collectors;
import com.example.backend.repository.RevenueRepository;
import com.example.backend.entity.Revenue;
import com.example.backend.entity.Order;
import com.example.backend.repository.OrderRepository;
import com.example.backend.repository.ReviewRepository;
import com.example.backend.entity.PlayerReview;
import com.example.backend.repository.PlayerReviewRepository;
import com.example.backend.dto.ReviewRequest;
import com.example.backend.dto.GamePlayerSummaryDTO;
import com.example.backend.service.OrderService;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/game-players")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Game Player", description = "Game player management APIs")
public class GamePlayerController {
    private final GamePlayerService gamePlayerService;
    private final GameRepository gameRepository;
    private final UserService userService;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final RevenueRepository revenueRepository;
    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final PlayerReviewRepository playerReviewRepository;
    private final OrderService orderService;
    private static final Logger log = LoggerFactory.getLogger(GamePlayerController.class);

    public GamePlayerController(GamePlayerService gamePlayerService, GameRepository gameRepository, UserService userService, PaymentRepository paymentRepository, NotificationService notificationService, RevenueRepository revenueRepository, ReviewRepository reviewRepository, OrderRepository orderRepository, PlayerReviewRepository playerReviewRepository, OrderService orderService) {
        this.gamePlayerService = gamePlayerService;
        this.gameRepository = gameRepository;
        this.userService = userService;
        this.paymentRepository = paymentRepository;
        this.notificationService = notificationService;
        this.revenueRepository = revenueRepository;
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.playerReviewRepository = playerReviewRepository;
        this.orderService = orderService;
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

    @Data
    public static class HireRequest {
        @NotNull(message = "User ID is required")
        private Long userId;

        @NotNull(message = "Hours is required")
        @Min(value = 1, message = "Hours must be at least 1")
        private Integer hours;

        @NotNull(message = "Coin is required")
        @Positive(message = "Coin must be positive")
        private Long coin;

        @NotNull(message = "Start time is required")
        private LocalDateTime startTime;

        @NotNull(message = "End time is required")
        private LocalDateTime endTime;

        private String specialRequest;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('PLAYER')")
    @Operation(summary = "Create a new game player")
    public ResponseEntity<ApiResponse<GamePlayer>> createGamePlayer(
            @Valid @RequestBody GamePlayerRequest request) {
        // Kiểm tra user đã có player chưa
        if (!gamePlayerService.getGamePlayersByUser(request.getUserId()).isEmpty()) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "User đã đăng ký làm player rồi!", null));
        }
        User user = userService.findById(request.getUserId());
        // Kiểm tra thông tin bắt buộc
        if (user.getFullName() == null || user.getDateOfBirth() == null ||
            user.getPhoneNumber() == null || user.getAddress() == null) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Bạn cần cập nhật đầy đủ thông tin cá nhân trước khi đăng ký làm player!", null));
        }
        Game game = gameRepository.findById(request.getGameId())
                .orElseThrow(() -> new ResourceNotFoundException("Game not found"));

        if (game.getHasRoles()) {
            if (request.getRole() == null || request.getRole().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Role is required for this game", null));
            }
            // Normalize role by trimming and converting to uppercase
            String normalizedRole = request.getRole().trim().toUpperCase();
            if (game.getAvailableRoles() != null) {
                boolean isValidRole = game.getAvailableRoles().stream()
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .anyMatch(role -> role.equals(normalizedRole));
                if (!isValidRole) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Invalid role for this game. Available roles: " + 
                            String.join(", ", game.getAvailableRoles()), null));
                }
                // Use normalized role for saving
                request.setRole(normalizedRole);
            }
        }
        // Tạo player từ thông tin user
        GamePlayer gamePlayer = gamePlayerService.createGamePlayer(
            user.getId(),
            request.getGameId(),
            request.getUsername(),
            request.getRank(),
            request.getRole(),
            request.getServer(),
            request.getPricePerHour(),
            request.getDescription()
        );
        return ResponseEntity.ok(new ApiResponse<>(true, "Game player created successfully", gamePlayer));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PLAYER')")
    @Operation(summary = "Update a game player")
    public ResponseEntity<ApiResponse<GamePlayer>> updateGamePlayer(
            @PathVariable Long id,
            @Valid @RequestBody GamePlayerRequest request) {
        GamePlayer gamePlayer = gamePlayerService.updateGamePlayer(
            id,
            request.getUserId(),
            request.getGameId(),
            request.getUsername(),
            request.getRank(),
            request.getRole(),
            request.getServer(),
            request.getPricePerHour(),
            request.getDescription()
        );
        return ResponseEntity.ok(new ApiResponse<>(true, "Game player updated successfully", gamePlayer));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a game player")
    public ResponseEntity<ApiResponse<Void>> deleteGamePlayer(@PathVariable Long id) {
        gamePlayerService.deleteGamePlayer(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Game player deleted successfully", null));
    }

    @GetMapping("/game/{gameId}")
    @Operation(summary = "Get game players by game")
    public ResponseEntity<ApiResponse<List<GamePlayer>>> getGamePlayersByGame(@PathVariable Long gameId) {
        List<GamePlayer> gamePlayers = gamePlayerService.getGamePlayersByGame(gameId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Game players retrieved successfully", gamePlayers));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get game players by user")
    public ResponseEntity<ApiResponse<List<GamePlayer>>> getGamePlayersByUser(@PathVariable Long userId) {
        List<GamePlayer> gamePlayers = gamePlayerService.getGamePlayersByUser(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Game players retrieved successfully", gamePlayers));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get game players by status")
    public ResponseEntity<ApiResponse<List<GamePlayer>>> getGamePlayersByStatus(@PathVariable String status) {
        List<GamePlayer> gamePlayers = gamePlayerService.getGamePlayersByStatus(status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Game players retrieved successfully", gamePlayers));
    }

    @GetMapping("/rank/{rank}")
    @Operation(summary = "Get game players by rank")
    public ResponseEntity<ApiResponse<List<GamePlayer>>> getGamePlayersByRank(@PathVariable String rank) {
        List<GamePlayer> gamePlayers = gamePlayerService.getGamePlayersByRank(rank);
        return ResponseEntity.ok(new ApiResponse<>(true, "Game players retrieved successfully", gamePlayers));
    }

    @GetMapping("/role/{role}")
    @Operation(summary = "Get game players by role")
    public ResponseEntity<ApiResponse<List<GamePlayer>>> getGamePlayersByRole(@PathVariable String role) {
        List<GamePlayer> gamePlayers = gamePlayerService.getGamePlayersByRole(role);
        return ResponseEntity.ok(new ApiResponse<>(true, "Game players retrieved successfully", gamePlayers));
    }

    @GetMapping("/server/{server}")
    @Operation(summary = "Get game players by server")
    public ResponseEntity<ApiResponse<List<GamePlayer>>> getGamePlayersByServer(@PathVariable String server) {
        List<GamePlayer> gamePlayers = gamePlayerService.getGamePlayersByServer(server);
        return ResponseEntity.ok(new ApiResponse<>(true, "Game players retrieved successfully", gamePlayers));
    }

    @GetMapping("/available")
    @Operation(summary = "Get available game players")
    public ResponseEntity<ApiResponse<List<GamePlayer>>> getAvailableGamePlayers() {
        List<GamePlayer> gamePlayers = gamePlayerService.getAvailableGamePlayers();
        return ResponseEntity.ok(new ApiResponse<>(true, "Available game players retrieved successfully", gamePlayers));
    }

    @PostMapping("/{id}/hire")
    @PreAuthorize("hasRole('USER')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Operation(summary = "Hire a game player")
    public ResponseEntity<ApiResponse<?>> hireGamePlayer(
            @PathVariable Long id,
            @Valid @RequestBody HireRequest request) {
        try {
            // Validate time
            if (request.getStartTime().isAfter(request.getEndTime())) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Thời gian bắt đầu phải trước thời gian kết thúc", null));
            }
            if (request.getStartTime().isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Thời gian bắt đầu phải sau thời gian hiện tại", null));
            }
            // Thêm điều kiện cách ít nhất 15 phút
            if (java.time.Duration.between(LocalDateTime.now(), request.getStartTime()).toMinutes() < 15) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Thời gian bắt đầu thuê phải cách thời điểm hiện tại ít nhất 15 phút", null));
            }

            // Check if player is available
            GamePlayer gamePlayer = gamePlayerService.findById(id);

            // Check if player is already hired in the requested time period
            List<Payment> activeHires = paymentRepository.findByPlayerIdAndStatusAndEndTimeAfter(
                gamePlayer.getUser().getId(), Payment.PaymentStatus.PENDING, LocalDateTime.now());
            boolean isOverlapping = activeHires.stream().anyMatch(payment ->
                request.getStartTime().isBefore(payment.getEndTime()) &&
                request.getEndTime().isAfter(payment.getStartTime())
            );
            if (isOverlapping) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Player đang được thuê trong khoảng thời gian này", null));
            }

            // Check user's coin balance
            User user = userService.findById(request.getUserId());
            if (user.getCoin() < request.getCoin()) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Số coin không đủ", null));
            }

            // Calculate hours from start and end time
            long hours = java.time.Duration.between(request.getStartTime(), request.getEndTime()).toHours();
            if (hours < 1) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Thời gian thuê phải ít nhất 1 giờ", null));
            }

            // Tạo đơn thuê (Order) thay vì Payment
            Order order = new Order();
            order.setRenter(user);
            order.setPlayer(gamePlayer);
            order.setStartTime(request.getStartTime());
            order.setEndTime(request.getEndTime());
            order.setPrice(request.getCoin());
            order.setStatus("PENDING");
            orderRepository.save(order);

            // Gửi notification cho player để xác nhận đơn thuê (có orderId)
            notificationService.createNotification(
                gamePlayer.getUser().getId(),
                "Bạn có đơn thuê mới!",
                "Bạn vừa nhận được yêu cầu thuê từ " + user.getUsername() + ". Vui lòng xác nhận đơn thuê trong hệ thống.",
                "rent",
                null,
                order.getId().toString()
            );
            // Gửi notification cho người thuê (user)
            notificationService.createNotification(
                user.getId(),
                "Đã gửi yêu cầu thuê player!",
                "Bạn đã gửi yêu cầu thuê player " + gamePlayer.getUsername() + ". Vui lòng chờ xác nhận từ player.",
                "rent_request",
                null,
                order.getId().toString()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("order", order);
            response.put("coin", order.getPrice());
            response.put("startTime", order.getStartTime());
            response.put("endTime", order.getEndTime());

            return ResponseEntity.ok(new ApiResponse<>(true, "Đã gửi yêu cầu thuê, chờ player xác nhận!", response));
        } catch (Exception e) {
            log.error("Error hiring game player: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Error hiring game player: " + e.getMessage(), null));
        }
    }

    @PostMapping("/order/{orderId}/confirm")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Operation(summary = "Player xác nhận đơn thuê (Order)")
    public ResponseEntity<ApiResponse<?>> confirmOrder(@PathVariable Long orderId, Authentication authentication) {
        try {
            User player = userService.findByUsername(authentication.getName());
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
            GamePlayer gamePlayer = order.getPlayer();

            // Kiểm tra quyền xác nhận (player phải là người được thuê)
            if (!gamePlayer.getUser().getId().equals(player.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "Bạn không có quyền xác nhận đơn này", null));
            }
            if (!"PENDING".equals(order.getStatus())) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Đơn thuê không ở trạng thái chờ xác nhận", null));
            }

            // Trừ coin, cộng coin, lưu revenue
            User user = order.getRenter();
            long totalCoin = order.getPrice();
            long playerReceive50 = Math.round(totalCoin * 0.5);
            // 40% sẽ cộng khi hoàn thành đơn
            long appRevenue = Math.round(totalCoin * 0.1); // 10% cho app
            if (user.getCoin() < totalCoin) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Người thuê không đủ coin", null));
            }
            user.setCoin(user.getCoin() - totalCoin);
            player.setCoin(player.getCoin() + playerReceive50);
            userService.save(user);
            userService.save(player);

            order.setStatus("CONFIRMED");
            orderRepository.save(order);

            gamePlayer.setStatus("HIRED");
            gamePlayer.setHiredBy(user);
            gamePlayer.setHireDate(order.getStartTime());
            gamePlayer.setReturnDate(order.getEndTime());
            gamePlayer.setHoursHired((int) java.time.Duration.between(order.getStartTime(), order.getEndTime()).toHours());
            gamePlayerService.save(gamePlayer);

            // Gửi notification cho người thuê khi player xác nhận đơn
            notificationService.createNotification(
                user.getId(),
                "Đơn thuê đã được xác nhận!",
                "Player " + gamePlayer.getUsername() + " đã xác nhận đơn thuê của bạn. Hãy chuẩn bị trải nghiệm dịch vụ!",
                "rent_confirm",
                null,
                order.getId().toString()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("order", order);
            response.put("coin", order.getPrice());
            response.put("startTime", order.getStartTime());
            response.put("endTime", order.getEndTime());

            return ResponseEntity.ok(new ApiResponse<>(true, "Đã xác nhận đơn thuê thành công!", response));
        } catch (Exception e) {
            log.error("Error confirming order: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Error confirming order: " + e.getMessage(), null));
        }
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Return a game player")
    public ResponseEntity<ApiResponse<GamePlayer>> returnGamePlayer(@PathVariable Long id) {
        GamePlayer gamePlayer = gamePlayerService.returnGamePlayer(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Game player returned successfully", gamePlayer));
    }

    @PutMapping("/{id}/rating")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update game player rating")
    public ResponseEntity<ApiResponse<GamePlayer>> updateRating(
            @PathVariable Long id,
            @RequestParam @Min(0) @Max(5) Double rating) {
        GamePlayer gamePlayer = gamePlayerService.updateRating(id, rating);
        return ResponseEntity.ok(new ApiResponse<>(true, "Game player rating updated successfully", gamePlayer));
    }

    @PutMapping("/{id}/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PLAYER')")
    @Operation(summary = "Update game player stats")
    public ResponseEntity<ApiResponse<GamePlayer>> updateStats(
            @PathVariable Long id,
            @RequestParam @Min(0) Integer totalGames,
            @RequestParam @Min(0) @Max(100) Integer winRate) {
        GamePlayer gamePlayer = gamePlayerService.updateStats(id, totalGames, winRate);
        return ResponseEntity.ok(new ApiResponse<>(true, "Game player stats updated successfully", gamePlayer));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GamePlayer>>> getAllGamePlayers() {
        List<GamePlayer> gamePlayers = gamePlayerService.getAllGamePlayers();
        return ResponseEntity.ok(new ApiResponse<>(true, "All game players retrieved successfully", gamePlayers));
    }

    @GetMapping("/{playerId}/status")
    public ResponseEntity<GamePlayerStatusResponse> getPlayerStatus(@PathVariable Long playerId) {
        try {
            GamePlayerStatusResponse status = gamePlayerService.getPlayerStatus(playerId);
            return ResponseEntity.ok(status);
        } catch (ResourceNotFoundException e) {
            log.error("Player not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (RuntimeException e) {
            log.error("Error getting player status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/order/{orderId}/cancel")
    @PreAuthorize("hasRole('USER')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Operation(summary = "User hủy đơn thuê (Order)")
    public ResponseEntity<ApiResponse<?>> cancelOrder(
            @PathVariable Long orderId,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
            GamePlayer gamePlayer = order.getPlayer();

            // Chỉ cho phép hủy khi đơn đang chờ xác nhận
            if (!"PENDING".equals(order.getStatus())) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Chỉ có thể hủy đơn khi đang chờ xác nhận", null));
            }

            // Hoàn coin
            user.setCoin(user.getCoin() + order.getPrice());
            userService.save(user);

            // Cập nhật trạng thái player
            gamePlayer.setStatus("AVAILABLE");
            gamePlayer.setHiredBy(null);
            gamePlayer.setHireDate(null);
            gamePlayer.setReturnDate(null);
            gamePlayer.setHoursHired(null);
            gamePlayerService.save(gamePlayer);

            // Cập nhật trạng thái đơn
            order.setStatus("CANCELED");
            orderRepository.save(order);

            // Xác định ai là người hủy đơn
            boolean isRenter = user.getId().equals(order.getRenter().getId());
            boolean isPlayer = user.getId().equals(order.getPlayer().getUser().getId());

            // Gửi notification phù hợp
            if (isPlayer) {
                // Player hủy đơn: gửi cho renter
                notificationService.createNotification(
                    order.getRenter().getId(),
                    "Đơn thuê bị từ chối",
                    "Đơn thuê của bạn đã bị từ chối bởi player.",
                    "rent_reject",
                    null,
                    order.getId().toString()
                );
            } else if (isRenter) {
                // Người thuê tự hủy: gửi cho chính họ
                notificationService.createNotification(
                    user.getId(),
                    "Hủy đơn thuê thành công",
                    "Bạn đã hủy đơn thuê thành công.",
                    "rent_cancel",
                    null,
                    order.getId().toString()
                );
            }

            Map<String, Object> response = new HashMap<>();
            response.put("gamePlayer", gamePlayer);
            response.put("order", order);
            response.put("refundedCoin", order.getPrice());

            return ResponseEntity.ok(new ApiResponse<>(true, "Hủy đơn thuê thành công", response));
        } catch (Exception e) {
            log.error("Error canceling order: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Error canceling order: " + e.getMessage(), null));
        }
    }

    @GetMapping("/hired")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get list of currently hired players")
    public ResponseEntity<ApiResponse<?>> getHiredPlayers(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            
            // Get all active payments for the user
            List<Payment> activeHires = paymentRepository.findByUserIdAndStatusAndEndTimeAfter(
                user.getId(), Payment.PaymentStatus.PENDING, LocalDateTime.now());

            List<Map<String, Object>> hiredPlayers = activeHires.stream()
                .map(payment -> {
                    Map<String, Object> playerInfo = new HashMap<>();
                    GamePlayer gamePlayer = payment.getGamePlayer();
                    playerInfo.put("gamePlayer", gamePlayer);
                    playerInfo.put("payment", payment);
                    playerInfo.put("startTime", payment.getStartTime());
                    playerInfo.put("endTime", payment.getEndTime());
                    playerInfo.put("coin", payment.getCoin());
                    return playerInfo;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse<>(true, "Successfully retrieved hired players", hiredPlayers));
        } catch (Exception e) {
            log.error("Error getting hired players: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Error getting hired players: " + e.getMessage(), null));
        }
    }

    @GetMapping("/hired-by-me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get list of players hired by the current user")
    public ResponseEntity<ApiResponse<?>> getPlayersHiredByMe(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            
            // Get all active payments where the user is the hirer
            List<Payment> activeHires = paymentRepository.findByUserIdAndStatusAndEndTimeAfter(
                user.getId(), Payment.PaymentStatus.PENDING, LocalDateTime.now());

            List<Map<String, Object>> hiredPlayers = activeHires.stream()
                .map(payment -> {
                    Map<String, Object> playerInfo = new HashMap<>();
                    GamePlayer gamePlayer = payment.getGamePlayer();
                    playerInfo.put("gamePlayer", gamePlayer);
                    playerInfo.put("payment", payment);
                    playerInfo.put("startTime", payment.getStartTime());
                    playerInfo.put("endTime", payment.getEndTime());
                    playerInfo.put("coin", payment.getCoin());
                    return playerInfo;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse<>(true, "Successfully retrieved players hired by you", hiredPlayers));
        } catch (Exception e) {
            log.error("Error getting players hired by user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Error getting players hired by you: " + e.getMessage(), null));
        }
    }

    @GetMapping("/hired-by-others")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get list of players that are currently hired by others")
    public ResponseEntity<ApiResponse<?>> getPlayersHiredByOthers(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            
            // Get all active payments where the user is the player
            List<Payment> activeHires = paymentRepository.findByPlayerIdAndStatusAndEndTimeAfter(
                user.getId(), Payment.PaymentStatus.PENDING, LocalDateTime.now());

            List<Map<String, Object>> hiredPlayers = activeHires.stream()
                .map(payment -> {
                    Map<String, Object> playerInfo = new HashMap<>();
                    GamePlayer gamePlayer = payment.getGamePlayer();
                    playerInfo.put("gamePlayer", gamePlayer);
                    playerInfo.put("payment", payment);
                    playerInfo.put("startTime", payment.getStartTime());
                    playerInfo.put("endTime", payment.getEndTime());
                    playerInfo.put("coin", payment.getCoin());
                    playerInfo.put("hirer", payment.getUser());
                    return playerInfo;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse<>(true, "Successfully retrieved players hired by others", hiredPlayers));
        } catch (Exception e) {
            log.error("Error getting players hired by others: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Error getting players hired by others: " + e.getMessage(), null));
        }
    }

    // API từ chối đơn thuê cho player
    @PostMapping("/hire/{paymentId}/reject")
    @PreAuthorize("hasRole('PLAYER')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Operation(summary = "Player từ chối đơn thuê")
    public ResponseEntity<ApiResponse<?>> rejectHire(@PathVariable Long paymentId, Authentication authentication) {
        try {
            User player = userService.findByUsername(authentication.getName());
            Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
            GamePlayer gamePlayer = payment.getGamePlayer();

            // Kiểm tra quyền từ chối
            if (!payment.getPlayer().getId().equals(player.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "Bạn không có quyền từ chối đơn này", null));
            }
            if (!Payment.PaymentStatus.PENDING.equals(payment.getStatus())) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Đơn thuê không ở trạng thái chờ xác nhận", null));
            }

            // Cập nhật trạng thái đơn thuê
            payment.setStatus(Payment.PaymentStatus.CANCELED);
            paymentRepository.save(payment);

            // Player vẫn giữ trạng thái AVAILABLE

            // Lấy user thuê từ payment
            User user = payment.getUser();
            // Gửi notification cho user khi bị từ chối đơn thuê
            notificationService.createNotification(
                user.getId(),
                "Đơn thuê bị từ chối",
                "Yêu cầu thuê player " + gamePlayer.getUsername() + " đã bị từ chối.",
                "rent_reject",
                null,
                payment.getId().toString()
            );

            return ResponseEntity.ok(new ApiResponse<>(true, "Đã từ chối đơn thuê thành công!", null));
        } catch (Exception e) {
            log.error("Error rejecting hire: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Error rejecting hire: " + e.getMessage(), null));
        }
    }

    @GetMapping("/revenue/total")
    public ResponseEntity<Long> getTotalRevenue() {
        Long total = revenueRepository.findAll().stream().mapToLong(r -> r.getAmount() != null ? r.getAmount() : 0L).sum();
        return ResponseEntity.ok(total);
    }

    @GetMapping("/revenue/growth-percent-yesterday")
    public ResponseEntity<Double> getRevenueGrowthPercentComparedToYesterday() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate yesterday = today.minusDays(1);
        java.time.LocalDateTime startToday = today.atStartOfDay();
        java.time.LocalDateTime endToday = today.atTime(23,59,59);
        java.time.LocalDateTime startYesterday = yesterday.atStartOfDay();
        java.time.LocalDateTime endYesterday = yesterday.atTime(23,59,59);
        Long todayRevenue = revenueRepository.sumRevenueByCreatedAtBetween(startToday, endToday);
        Long yesterdayRevenue = revenueRepository.sumRevenueByCreatedAtBetween(startYesterday, endYesterday);
        if (todayRevenue == null) todayRevenue = 0L;
        if (yesterdayRevenue == null) yesterdayRevenue = 0L;
        double percent;
        if (yesterdayRevenue == 0) {
            percent = todayRevenue > 0 ? 100.0 : 0.0;
        } else {
            percent = ((double)(todayRevenue - yesterdayRevenue) / yesterdayRevenue) * 100.0;
        }
        return ResponseEntity.ok(percent);
    }

    @PostMapping("/order/{orderId}/complete")
    @PreAuthorize("hasRole('USER') or hasRole('PLAYER')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Operation(summary = "Hoàn thành đơn thuê (Order)")
    public ResponseEntity<ApiResponse<?>> completeOrder(@PathVariable Long orderId, Authentication authentication) {
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
            GamePlayer gamePlayer = order.getPlayer();

            // Chỉ cho phép hoàn thành khi trạng thái là CONFIRMED
            if (!"CONFIRMED".equals(order.getStatus())) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Chỉ có thể hoàn thành đơn khi đã xác nhận", null));
            }

            // Cập nhật trạng thái đơn và player
            order.setStatus("COMPLETED");
            orderRepository.save(order);

            // Cộng nốt 40% coin cho player khi hoàn thành đơn
            long totalCoin = order.getPrice();
            long playerReceive40 = Math.round(totalCoin * 0.4);
            User playerUser = gamePlayer.getUser();
            playerUser.setCoin(playerUser.getCoin() + playerReceive40);
            userService.save(playerUser);

            gamePlayer.setStatus("AVAILABLE");
            gamePlayer.setHiredBy(null);
            gamePlayer.setHireDate(null);
            gamePlayer.setReturnDate(null);
            gamePlayer.setHoursHired(null);
            gamePlayerService.save(gamePlayer);

            // Gửi notification cho user và player
            notificationService.createNotification(
                order.getRenter().getId(),
                "Đơn thuê đã hoàn thành",
                "Đơn thuê của bạn đã được hoàn thành.",
                "rent_complete",
                null,
                order.getId().toString()
            );
            notificationService.createNotification(
                gamePlayer.getUser().getId(),
                "Đơn thuê đã hoàn thành",
                "Bạn đã hoàn thành đơn thuê với " + order.getRenter().getUsername() + ".",
                "rent_complete",
                null,
                order.getId().toString()
            );

            // Gửi thông báo đánh giá cho người thuê
            orderService.sendReviewNotificationForCompletedOrder(order);

            Map<String, Object> response = new HashMap<>();
            response.put("order", order);
            response.put("gamePlayer", gamePlayer);

            return ResponseEntity.ok(new ApiResponse<>(true, "Đơn thuê đã hoàn thành", response));
        } catch (Exception e) {
            log.error("Error completing order: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Error completing order: " + e.getMessage(), null));
        }
    }

    @PostMapping("/order/{orderId}/review")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Đánh giá player cho đơn thuê (Order)")
    public ResponseEntity<ApiResponse<?>> reviewOrder(@PathVariable Long orderId, @Valid @RequestBody ReviewRequest request, Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
            GamePlayer gamePlayer = order.getPlayer();

            // Chỉ cho phép đánh giá khi đơn đã hoàn thành
            if (!"COMPLETED".equals(order.getStatus())) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Chỉ có thể đánh giá sau khi đơn đã hoàn thành", null));
            }

            // Kiểm tra user là người thuê
            if (!order.getRenter().getId().equals(user.getId())) {
                return ResponseEntity.status(403)
                    .body(new ApiResponse<>(false, "Không có quyền đánh giá đơn này", null));
            }

            // Kiểm tra đã đánh giá chưa
            if (playerReviewRepository.existsByOrderId(orderId)) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Bạn đã đánh giá đơn này rồi", null));
            }

            PlayerReview review = new PlayerReview();
            review.setOrder(order);
            review.setGamePlayer(gamePlayer);
            review.setUser(user);
            review.setRating(request.getRating());
            review.setComment(request.getComment());
            review.setCreatedAt(java.time.LocalDateTime.now());
            playerReviewRepository.save(review);

            // Gửi notification cho player
            notificationService.createNotification(
                gamePlayer.getUser().getId(),
                "Bạn nhận được đánh giá mới!",
                "Bạn vừa nhận được đánh giá từ " + user.getUsername() + ".",
                "review",
                null,
                order.getId().toString()
            );

            return ResponseEntity.ok(new ApiResponse<>(true, "Đánh giá thành công!", review));
        } catch (Exception e) {
            log.error("Error review order: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Error review order: " + e.getMessage(), null));
        }
    }

    @PostMapping("/ban/{playerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Operation(summary = "Ban player")
    public ResponseEntity<ApiResponse<?>> banPlayer(@PathVariable Long playerId, Authentication authentication) {
        try {
            User admin = userService.findByUsername(authentication.getName());
            GamePlayer player = gamePlayerService.findById(playerId);

            // Hủy các đơn thuê PENDING/CONFIRMED của player này
            List<Order> ordersToCancel = orderRepository.findAll().stream()
                .filter(o -> o.getPlayer() != null && o.getPlayer().getId().equals(player.getId()))
                .filter(o -> "PENDING".equalsIgnoreCase(o.getStatus()) || "CONFIRMED".equalsIgnoreCase(o.getStatus()))
                .toList();
            for (Order order : ordersToCancel) {
                // Hoàn coin cho user nếu chưa hoàn thành
                if (order.getRenter() != null && order.getPrice() != null && order.getPrice() > 0) {
                    order.getRenter().setCoin(order.getRenter().getCoin() + order.getPrice());
                    userService.save(order.getRenter());
                }
                order.setStatus("CANCELED");
                orderRepository.save(order);
                // Gửi notification cho user
                if (order.getRenter() != null) {
                    notificationService.createNotification(
                        order.getRenter().getId(),
                        "Đơn thuê bị hủy do player vi phạm",
                        "Đơn thuê với player này đã bị hủy và bạn đã được hoàn coin.",
                        "order_cancel_ban",
                        null,
                        order.getId().toString()
                    );
                }
            }

            // Cập nhật trạng thái player
            player.setStatus("BANNED");
            gamePlayerService.save(player);

            return ResponseEntity.ok(new ApiResponse<>(true, "Player đã bị ban", null));
        } catch (Exception e) {
            log.error("Error banning player: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Error banning player: " + e.getMessage(), null));
        }
    }

    @PostMapping("/unban/{playerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Operation(summary = "Unban player")
    public ResponseEntity<ApiResponse<?>> unbanPlayer(@PathVariable Long playerId, Authentication authentication) {
        try {
            User admin = userService.findByUsername(authentication.getName());
            GamePlayer player = gamePlayerService.findById(playerId);
            if (!"BANNED".equalsIgnoreCase(player.getStatus())) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Player không bị ban", null));
            }
            player.setStatus("AVAILABLE");
            gamePlayerService.save(player);
            return ResponseEntity.ok(new ApiResponse<>(true, "Player đã được mở ban", null));
        } catch (Exception e) {
            log.error("Error unbanning player: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Error unbanning player: " + e.getMessage(), null));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<List<GamePlayerSummaryDTO>>> getGamePlayerSummary() {
        List<GamePlayer> gamePlayers = gamePlayerService.getAllGamePlayers();
        List<GamePlayerSummaryDTO> result = new ArrayList<>();
        for (GamePlayer gp : gamePlayers) {
            GamePlayerSummaryDTO dto = new GamePlayerSummaryDTO();
            dto.setId(gp.getId());
            dto.setName(gp.getUser() != null ? gp.getUser().getFullName() : "");
            dto.setEmail(gp.getUser() != null ? gp.getUser().getEmail() : "");
            // Số đơn thuê
            int totalOrders = orderRepository.findAll().stream().filter(o -> o.getPlayer() != null && o.getPlayer().getId().equals(gp.getId())).toList().size();
            dto.setTotalOrders(totalOrders);
            // Số đánh giá
            int totalReviews = playerReviewRepository.findByGamePlayerUserId(gp.getUser().getId()).size();
            dto.setTotalReviews(totalReviews);
            // Thu nhập: tổng price các order hoàn thành
            long totalRevenue = orderRepository.findAll().stream()
                .filter(o -> o.getPlayer() != null && o.getPlayer().getId().equals(gp.getId()) && "COMPLETED".equalsIgnoreCase(o.getStatus()))
                .mapToLong(o -> o.getPrice() != null ? o.getPrice() : 0L).sum();
            dto.setTotalRevenue(totalRevenue);
            // Trạng thái
            dto.setStatus(gp.getStatus());
            // Xếp hạng (label)
            dto.setRankLabel(gp.getRank());
            // Rating (sao)
            dto.setRating(gp.getRating() != null ? gp.getRating() : 0.0);
            // Thể loại game
            dto.setGameName(gp.getGame() != null ? gp.getGame().getName() : "");
            result.add(dto);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Game player summary", result));
    }
} 