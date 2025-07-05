package com.example.backend.controller;

import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.ReviewRequest;
import com.example.backend.entity.Order;
import com.example.backend.entity.PlayerReview;
import com.example.backend.entity.User;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.OrderRepository;
import com.example.backend.repository.PlayerReviewRepository;
import com.example.backend.service.NotificationService;
import com.example.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/order-reviews")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Order Review", description = "Order review management APIs")
public class OrderReviewController {

    private final PlayerReviewRepository playerReviewRepository;
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    public OrderReviewController(PlayerReviewRepository playerReviewRepository, 
                               OrderRepository orderRepository, 
                               UserService userService, 
                               NotificationService notificationService) {
        this.playerReviewRepository = playerReviewRepository;
        this.orderRepository = orderRepository;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @PostMapping("/{orderId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Đánh giá player cho đơn hàng")
    public ResponseEntity<ApiResponse<?>> createOrderReview(
            @Parameter(description = "Order ID") @PathVariable Long orderId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

            // Kiểm tra quyền đánh giá (chỉ người thuê mới được đánh giá)
            if (!order.getRenter().getId().equals(user.getId())) {
                return ResponseEntity.status(403)
                    .body(new ApiResponse<>(false, "Bạn không có quyền đánh giá đơn hàng này", null));
            }

            // Kiểm tra trạng thái đơn hàng
            if (!"COMPLETED".equals(order.getStatus())) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Chỉ có thể đánh giá sau khi đơn hàng đã hoàn thành", null));
            }

            // Kiểm tra đã đánh giá chưa
            if (playerReviewRepository.existsByOrderId(orderId)) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Bạn đã đánh giá đơn hàng này rồi", null));
            }

            // Tạo đánh giá mới
            PlayerReview review = new PlayerReview();
            review.setOrder(order);
            review.setGamePlayer(order.getPlayer());
            review.setUser(user);
            review.setRating(request.getRating());
            review.setComment(request.getComment());
            review.setCreatedAt(LocalDateTime.now());
            
            PlayerReview savedReview = playerReviewRepository.save(review);

            // Gửi thông báo cho player
            if (order.getPlayer() != null && order.getPlayer().getUser() != null) {
                notificationService.createNotification(
                    order.getPlayer().getUser().getId(),
                    "Bạn nhận được đánh giá mới!",
                    "Bạn vừa nhận được đánh giá từ " + user.getUsername() + " cho đơn hàng #" + orderId,
                    "review",
                    null,
                    orderId
                );
            }

            Map<String, Object> response = new HashMap<>();
            response.put("review", savedReview);
            response.put("message", "Đánh giá thành công!");

            return ResponseEntity.ok(new ApiResponse<>(true, "Đánh giá thành công!", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Lỗi khi tạo đánh giá: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Lấy đánh giá của đơn hàng")
    public ResponseEntity<ApiResponse<?>> getOrderReview(
            @Parameter(description = "Order ID") @PathVariable Long orderId) {
        try {
            PlayerReview review = playerReviewRepository.findByOrderId(orderId);
            if (review == null) {
                return ResponseEntity.ok(new ApiResponse<>(true, "Đơn hàng chưa có đánh giá", null));
            }

            Map<String, Object> reviewData = new HashMap<>();
            reviewData.put("id", review.getId());
            reviewData.put("rating", review.getRating());
            reviewData.put("comment", review.getComment());
            reviewData.put("reviewerName", review.getUser().getUsername());
            reviewData.put("reviewerAvatar", review.getUser().getAvatarUrl());
            reviewData.put("createdAt", review.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            return ResponseEntity.ok(new ApiResponse<>(true, "Lấy đánh giá thành công", reviewData));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Lỗi khi lấy đánh giá: " + e.getMessage(), null));
        }
    }

    @PutMapping("/{orderId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Cập nhật đánh giá đơn hàng")
    public ResponseEntity<ApiResponse<?>> updateOrderReview(
            @Parameter(description = "Order ID") @PathVariable Long orderId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            PlayerReview review = playerReviewRepository.findByOrderId(orderId);
            
            if (review == null) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Không tìm thấy đánh giá cho đơn hàng này", null));
            }

            // Kiểm tra quyền cập nhật
            if (!review.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403)
                    .body(new ApiResponse<>(false, "Bạn không có quyền cập nhật đánh giá này", null));
            }

            // Cập nhật đánh giá
            review.setRating(request.getRating());
            review.setComment(request.getComment());
            
            PlayerReview updatedReview = playerReviewRepository.save(review);

            Map<String, Object> response = new HashMap<>();
            response.put("review", updatedReview);
            response.put("message", "Cập nhật đánh giá thành công!");

            return ResponseEntity.ok(new ApiResponse<>(true, "Cập nhật đánh giá thành công!", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Lỗi khi cập nhật đánh giá: " + e.getMessage(), null));
        }
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Xóa đánh giá đơn hàng")
    public ResponseEntity<ApiResponse<?>> deleteOrderReview(
            @Parameter(description = "Order ID") @PathVariable Long orderId,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            PlayerReview review = playerReviewRepository.findByOrderId(orderId);
            
            if (review == null) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Không tìm thấy đánh giá cho đơn hàng này", null));
            }

            // Kiểm tra quyền xóa
            if (!review.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403)
                    .body(new ApiResponse<>(false, "Bạn không có quyền xóa đánh giá này", null));
            }

            playerReviewRepository.delete(review);

            return ResponseEntity.ok(new ApiResponse<>(true, "Xóa đánh giá thành công!", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Lỗi khi xóa đánh giá: " + e.getMessage(), null));
        }
    }

    @GetMapping("/player/{playerId}")
    @Operation(summary = "Lấy tất cả đánh giá mà player nhận được")
    public ResponseEntity<ApiResponse<?>> getPlayerReviews(
            @Parameter(description = "Player ID (GamePlayer ID)") @PathVariable Long playerId) {
        try {
            // Sử dụng gamePlayerId thay vì userId để lấy đánh giá mà player nhận được
            List<PlayerReview> reviews = playerReviewRepository.findByGamePlayerId(playerId);
            Double averageRating = playerReviewRepository.getAverageRatingByGamePlayerId(playerId);

            List<Map<String, Object>> reviewList = reviews.stream().map(review -> {
                Map<String, Object> reviewData = new HashMap<>();
                reviewData.put("id", review.getId());
                reviewData.put("rating", review.getRating());
                reviewData.put("comment", review.getComment());
                reviewData.put("reviewerName", review.getUser().getUsername());
                reviewData.put("reviewerAvatar", review.getUser().getAvatarUrl());
                reviewData.put("orderId", review.getOrder().getId());
                reviewData.put("createdAt", review.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                return reviewData;
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("reviews", reviewList);
            response.put("averageRating", averageRating != null ? averageRating : 0.0);
            response.put("totalReviews", reviews.size());

            return ResponseEntity.ok(new ApiResponse<>(true, "Lấy đánh giá player thành công", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Lỗi khi lấy đánh giá player: " + e.getMessage(), null));
        }
    }

    @GetMapping("/user/reviews")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Lấy tất cả đánh giá mà user hiện tại nhận được")
    public ResponseEntity<ApiResponse<?>> getUserReviews(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            List<PlayerReview> reviews = playerReviewRepository.findByGamePlayerUserId(user.getId());

            List<Map<String, Object>> reviewList = reviews.stream().map(review -> {
                Map<String, Object> reviewData = new HashMap<>();
                reviewData.put("id", review.getId());
                reviewData.put("rating", review.getRating());
                reviewData.put("comment", review.getComment());
                reviewData.put("orderId", review.getOrder().getId());
                reviewData.put("reviewerName", review.getUser().getUsername());
                reviewData.put("reviewerAvatar", review.getUser().getAvatarUrl());
                reviewData.put("gameName", review.getGamePlayer().getGame() != null ? review.getGamePlayer().getGame().getName() : "");
                reviewData.put("createdAt", review.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                return reviewData;
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("reviews", reviewList);
            response.put("totalReviews", reviews.size());

            return ResponseEntity.ok(new ApiResponse<>(true, "Lấy đánh giá của user thành công", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Lỗi khi lấy đánh giá user: " + e.getMessage(), null));
        }
    }

    @GetMapping("/user/{userId}/reviews")
    @Operation(summary = "Lấy tất cả đánh giá mà một user nhận được")
    public ResponseEntity<ApiResponse<?>> getUserReviewsById(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        try {
            List<PlayerReview> reviews = playerReviewRepository.findByGamePlayerUserId(userId);
            Double averageRating = playerReviewRepository.getAverageRatingByPlayerId(userId);

            List<Map<String, Object>> reviewList = reviews.stream().map(review -> {
                Map<String, Object> reviewData = new HashMap<>();
                reviewData.put("id", review.getId());
                reviewData.put("rating", review.getRating());
                reviewData.put("comment", review.getComment());
                reviewData.put("orderId", review.getOrder().getId());
                reviewData.put("reviewerName", review.getUser().getUsername());
                reviewData.put("reviewerAvatar", review.getUser().getAvatarUrl());
                reviewData.put("gameName", review.getGamePlayer().getGame() != null ? review.getGamePlayer().getGame().getName() : "");
                reviewData.put("createdAt", review.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                return reviewData;
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("reviews", reviewList);
            response.put("averageRating", averageRating != null ? averageRating : 0.0);
            response.put("totalReviews", reviews.size());

            return ResponseEntity.ok(new ApiResponse<>(true, "Lấy đánh giá của user thành công", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Lỗi khi lấy đánh giá user: " + e.getMessage(), null));
        }
    }

    @GetMapping("/user/given-reviews")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Lấy tất cả đánh giá mà user đã viết")
    public ResponseEntity<ApiResponse<?>> getUserGivenReviews(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            List<PlayerReview> reviews = playerReviewRepository.findByUserId(user.getId());

            List<Map<String, Object>> reviewList = reviews.stream().map(review -> {
                Map<String, Object> reviewData = new HashMap<>();
                reviewData.put("id", review.getId());
                reviewData.put("rating", review.getRating());
                reviewData.put("comment", review.getComment());
                reviewData.put("orderId", review.getOrder().getId());
                reviewData.put("playerName", review.getGamePlayer().getUser().getUsername());
                reviewData.put("playerAvatar", review.getGamePlayer().getUser().getAvatarUrl());
                reviewData.put("gameName", review.getGamePlayer().getGame() != null ? review.getGamePlayer().getGame().getName() : "");
                reviewData.put("createdAt", review.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                return reviewData;
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("reviews", reviewList);
            response.put("totalReviews", reviews.size());

            return ResponseEntity.ok(new ApiResponse<>(true, "Lấy đánh giá đã viết thành công", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Lỗi khi lấy đánh giá đã viết: " + e.getMessage(), null));
        }
    }

    @GetMapping("/stats/player/{playerId}")
    @Operation(summary = "Lấy thống kê đánh giá của player")
    public ResponseEntity<ApiResponse<?>> getPlayerReviewStats(
            @Parameter(description = "Player ID (GamePlayer ID)") @PathVariable Long playerId) {
        try {
            List<PlayerReview> reviews = playerReviewRepository.findByGamePlayerId(playerId);
            Double averageRating = playerReviewRepository.getAverageRatingByGamePlayerId(playerId);

            // Tính phân bố rating
            Map<Integer, Long> ratingDistribution = reviews.stream()
                .collect(Collectors.groupingBy(PlayerReview::getRating, Collectors.counting()));

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalReviews", reviews.size());
            stats.put("averageRating", averageRating != null ? averageRating : 0.0);
            stats.put("ratingDistribution", ratingDistribution);

            // Tính rating theo sao
            for (int i = 1; i <= 5; i++) {
                if (!ratingDistribution.containsKey(i)) {
                    ratingDistribution.put(i, 0L);
                }
            }

            return ResponseEntity.ok(new ApiResponse<>(true, "Lấy thống kê đánh giá thành công", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Lỗi khi lấy thống kê đánh giá: " + e.getMessage(), null));
        }
    }

    @GetMapping("/rating-summary/player/{playerId}")
    @Operation(summary = "Lấy tổng quan rating của player (trung bình sao và số lượt đánh giá)")
    public ResponseEntity<ApiResponse<?>> getPlayerRatingSummary(
            @Parameter(description = "Player ID (GamePlayer ID)") @PathVariable Long playerId) {
        try {
            List<PlayerReview> reviews = playerReviewRepository.findByGamePlayerId(playerId);
            Double averageRating = playerReviewRepository.getAverageRatingByGamePlayerId(playerId);

            Map<String, Object> summary = new HashMap<>();
            summary.put("averageRating", averageRating != null ? averageRating : 0.0);
            summary.put("totalReviews", reviews.size());
            summary.put("playerId", playerId);

            return ResponseEntity.ok(new ApiResponse<>(true, "Lấy tổng quan rating thành công", summary));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Lỗi khi lấy tổng quan rating: " + e.getMessage(), null));
        }
    }

    @GetMapping("/rating-summary/user/{userId}")
    @Operation(summary = "Lấy tổng quan rating của user (trung bình sao và số lượt đánh giá)")
    public ResponseEntity<ApiResponse<?>> getUserRatingSummary(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        try {
            List<PlayerReview> reviews = playerReviewRepository.findByGamePlayerUserId(userId);
            Double averageRating = playerReviewRepository.getAverageRatingByPlayerId(userId);

            Map<String, Object> summary = new HashMap<>();
            summary.put("averageRating", averageRating != null ? averageRating : 0.0);
            summary.put("totalReviews", reviews.size());
            summary.put("userId", userId);

            return ResponseEntity.ok(new ApiResponse<>(true, "Lấy tổng quan rating thành công", summary));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Lỗi khi lấy tổng quan rating: " + e.getMessage(), null));
        }
    }

    @GetMapping("/pending-reviews")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Lấy danh sách đơn hàng cần đánh giá")
    public ResponseEntity<ApiResponse<?>> getPendingReviews(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            
            // Lấy tất cả đơn hàng đã hoàn thành của user
            List<Order> completedOrders = orderRepository.findAll().stream()
                .filter(order -> order.getRenter() != null && order.getRenter().getId().equals(user.getId()))
                .filter(order -> "COMPLETED".equals(order.getStatus()))
                .collect(Collectors.toList());

            // Lọc ra những đơn hàng chưa có đánh giá
            List<Map<String, Object>> pendingReviews = completedOrders.stream()
                .filter(order -> !playerReviewRepository.existsByOrderId(order.getId()))
                .map(order -> {
                    Map<String, Object> orderData = new HashMap<>();
                    orderData.put("orderId", order.getId());
                    orderData.put("playerName", order.getPlayer() != null && order.getPlayer().getUser() != null ? 
                                order.getPlayer().getUser().getUsername() : "");
                    orderData.put("playerAvatar", order.getPlayer() != null && order.getPlayer().getUser() != null ? 
                                order.getPlayer().getUser().getAvatarUrl() : "");
                    orderData.put("gameName", order.getPlayer() != null && order.getPlayer().getGame() != null ? 
                                order.getPlayer().getGame().getName() : "");
                    orderData.put("playerRank", order.getPlayer() != null ? order.getPlayer().getRank() : "");
                    orderData.put("startTime", order.getStartTime() != null ? 
                                order.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
                    orderData.put("endTime", order.getEndTime() != null ? 
                                order.getEndTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
                    orderData.put("price", order.getPrice());
                    return orderData;
                })
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("pendingReviews", pendingReviews);
            response.put("totalPending", pendingReviews.size());

            return ResponseEntity.ok(new ApiResponse<>(true, "Lấy danh sách đơn hàng cần đánh giá thành công", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Lỗi khi lấy danh sách đơn hàng cần đánh giá: " + e.getMessage(), null));
        }
    }
} 