package com.example.backend.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.ReviewRequest;
import com.example.backend.dto.TopupUserDTO;
import com.example.backend.entity.Order;
import com.example.backend.entity.Payment;
import com.example.backend.entity.PlayerReview;
import com.example.backend.entity.User;
import com.example.backend.exception.PaymentException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.OrderRepository;
import com.example.backend.repository.PaymentRepository;
import com.example.backend.repository.PlayerReviewRepository;
import com.example.backend.service.NotificationService;
import com.example.backend.service.PaymentService;
import com.example.backend.service.QRCodeService;
import com.example.backend.service.UserService;
import com.example.backend.service.VnPayService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Payment", description = "Payment management APIs")
public class PaymentController {
    private final PaymentService paymentService;
    private final UserService userService;
    private final PaymentRepository paymentRepository;
    private final QRCodeService qrCodeService;
    private final PlayerReviewRepository playerReviewRepository;
    private final NotificationService notificationService;
    private final VnPayService vnPayService;
    private final OrderRepository orderRepository;
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    public PaymentController(PaymentService paymentService, UserService userService,
            PaymentRepository paymentRepository, QRCodeService qrCodeService,
            PlayerReviewRepository playerReviewRepository,
            NotificationService notificationService,
            VnPayService vnPayService,
            OrderRepository orderRepository) {
        this.paymentService = paymentService;
        this.userService = userService;
        this.paymentRepository = paymentRepository;
        this.qrCodeService = qrCodeService;
        this.playerReviewRepository = playerReviewRepository;
        this.notificationService = notificationService;
        this.vnPayService = vnPayService;
        this.orderRepository = orderRepository;
    }

    @Operation(summary = "Create a new payment")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Payment> createPayment(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Payment payment = paymentService.createPayment(
                    request.getGamePlayerId(),
                    user.getId(),
                    BigDecimal.valueOf(request.getCoin()),
                    "COIN",
                    request.getPaymentMethod());
            return ResponseEntity.ok(payment);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Process a payment")
    @PostMapping("/{id}/process")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Payment> processPayment(
            @Parameter(description = "Payment ID") @PathVariable Long id,
            @Valid @RequestBody ProcessPaymentRequest request) {
        try {
            Payment payment = paymentService.processPayment(id, request.getTransactionId());
            return ResponseEntity.ok(payment);
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Refund a payment")
    @PostMapping("/{id}/refund")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Payment> refundPayment(
            @Parameter(description = "Payment ID") @PathVariable Long id,
            @Valid @RequestBody RefundRequest request) {
        try {
            Payment payment = paymentService.refundPayment(id, request.getReason());
            return ResponseEntity.ok(payment);
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Get user payments")
    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Payment>> getUserPayments(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        return ResponseEntity.ok(paymentService.getUserPayments(user.getId()));
    }

    @Operation(summary = "Get game player payments")
    @GetMapping("/game-player/{gamePlayerId}")
    public ResponseEntity<List<Payment>> getGamePlayerPayments(
            @Parameter(description = "Game player ID") @PathVariable Long gamePlayerId) {
        return ResponseEntity.ok(paymentService.getGamePlayerPayments(gamePlayerId));
    }

    @Operation(summary = "Get payments by status")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Payment>> getPaymentsByStatus(
            @Parameter(description = "Payment status") @PathVariable String status) {
        try {
            Payment.PaymentStatus paymentStatus = Payment.PaymentStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(paymentService.getPaymentsByStatus(paymentStatus.name()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Get payments by date range")
    @GetMapping("/date-range")
    public ResponseEntity<List<Payment>> getPaymentsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(paymentService.getPaymentsByDateRange(start, end));
    }

    @PostMapping("/topup")
    @PreAuthorize("isAuthenticated()")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResponseEntity<?> topUp(@Valid @RequestBody TopUpRequest request, Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body("Không tìm thấy người dùng");
            }

            // Kiểm tra số coin
            if (request.getCoin() == null || request.getCoin() <= 0) {
                return ResponseEntity.badRequest().body("Số coin phải lớn hơn 0");
            }

            // Cộng coin vào tài khoản
            user.setCoin(user.getCoin() + request.getCoin());
            user = userService.save(user);

            // Tạo bản ghi thanh toán
            Payment payment = new Payment();
            payment.setUser(user);
            payment.setCoin(request.getCoin());
            payment.setCurrency("COIN");
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setPaymentMethod(Payment.PaymentMethod.TOPUP);
            payment.setType(Payment.PaymentType.TOPUP);
            payment.setCreatedAt(java.time.LocalDateTime.now());
            paymentRepository.save(payment);

            // Gửi notification khi nạp tiền thành công
            notificationService.createNotification(
                    user.getId(),
                    "Nạp xu thành công!",
                    "Bạn vừa nạp thành công " + request.getCoin() + " xu vào tài khoản.",
                    "topup",
                    null,
                    payment.getId().toString());

            return ResponseEntity.ok(Map.of(
                    "message", "Nạp coin thành công",
                    "coin", request.getCoin()));
        } catch (Exception e) {
            logger.error("Lỗi khi nạp coin: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Lỗi khi nạp coin: " + e.getMessage());
        }
    }

    @GetMapping("/hire/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getHireHistory(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        List<Payment> hires = paymentRepository.findByUserIdAndTypeOrderByCreatedAtDesc(user.getId(),
                Payment.PaymentType.HIRE);
        return ResponseEntity.ok(hires);
    }

    @GetMapping("/hire/player/{playerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPlayerHireHistory(@PathVariable Long playerId) {
        List<Payment> hires = paymentRepository.findByPlayerIdAndTypeOrderByCreatedAtDesc(playerId,
                Payment.PaymentType.HIRE);
        return ResponseEntity.ok(hires);
    }

    @PostMapping("/hire/{paymentId}/review")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> reviewPlayer(
            @PathVariable Long paymentId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        User reviewer = userService.findByUsername(authentication.getName());
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        // Kiểm tra quyền đánh giá
        if (!payment.getUser().getId().equals(reviewer.getId())) {
            return ResponseEntity.status(403).body("Không có quyền đánh giá");
        }

        // Kiểm tra thời gian đánh giá
        if (payment.getEndTime().isAfter(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Chưa thể đánh giá, hợp đồng chưa kết thúc");
        }

        Order order = orderRepository.findByPaymentId(paymentId);
        if (order == null) {
            return ResponseEntity.badRequest().body("Không tìm thấy đơn thuê tương ứng với paymentId");
        }
        if (playerReviewRepository.existsByOrderId(order.getId())) {
            return ResponseEntity.badRequest().body("Đã đánh giá cho hợp đồng này");
        }

        // Tạo đánh giá
        PlayerReview review = new PlayerReview();
        review.setGamePlayer(payment.getGamePlayer());
        review.setUser(reviewer);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        playerReviewRepository.save(review);

        return ResponseEntity.ok("Đánh giá thành công");
    }

    @GetMapping("/hire/player/{playerId}/reviews")
    public ResponseEntity<?> getPlayerReviews(@PathVariable Long playerId) {
        List<PlayerReview> reviews = playerReviewRepository.findByGamePlayerUserId(playerId);
        Double averageRating = playerReviewRepository.getAverageRatingByPlayerId(playerId);
        int reviewCount = reviews.size();
        return ResponseEntity.ok(Map.of(
                "reviews", reviews,
                "averageRating", averageRating != null ? averageRating : 0.0,
                "reviewCount", reviewCount));
    }

    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('PLAYER')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResponseEntity<?> withdraw(@RequestBody WithdrawRequest request, Authentication authentication) {
        try {
            User player = userService.findByUsername(authentication.getName());
            Long coin = request.getCoin();

            if (coin == null || coin <= 0) {
                return ResponseEntity.badRequest().body("Số coin không hợp lệ");
            }

            if (player.getCoin() < coin) {
                return ResponseEntity.badRequest().body("Số coin không đủ");
            }

            player.setCoin(player.getCoin() - coin);
            userService.save(player);

            Payment payment = new Payment();
            payment.setUser(player);
            payment.setCoin(coin);
            payment.setCurrency("COIN");
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setPaymentMethod(Payment.PaymentMethod.WITHDRAW);
            payment.setType(Payment.PaymentType.WITHDRAW);
            payment.setCreatedAt(java.time.LocalDateTime.now());
            paymentRepository.save(payment);

            // Gửi notification khi rút tiền thành công
            notificationService.createNotification(
                    player.getId(),
                    "Rút coin thành công",
                    "Bạn đã rút thành công " + coin + " coin.",
                    "withdraw",
                    null,
                    payment.getId().toString());

            return ResponseEntity.ok(Map.of(
                    "message", "Rút coin thành công",
                    "coin", coin));
        } catch (Exception e) {
            logger.error("Lỗi khi rút coin: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Lỗi khi rút coin: " + e.getMessage());
        }
    }

    @Operation(summary = "Get user wallet balance")
    @GetMapping("/wallet-balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> getWalletBalance(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        return ResponseEntity.ok(user.getCoin());
    }

    @Operation(summary = "Get all balance change history for current user")
    @GetMapping("/balance-history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Payment>> getBalanceHistory(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        List<Payment> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return ResponseEntity.ok(payments);
    }

    @PostMapping("/deposit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deposit(@RequestBody DepositRequest request, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        DepositResponse response = new DepositResponse();

        // Validate số coin
        if (request.getCoin() == null || request.getCoin() <= 0) {
            response.setMessage("Số coin nạp tối thiểu là 1");
            return ResponseEntity.badRequest().body(response);
        }

        String method = request.getMethod().toUpperCase();
        String transactionId = "TXN_" + System.currentTimeMillis();

        try {
            switch (method) {
                case "MOMO":
                case "VNPAY":
                case "ZALOPAY":
                    String qrCode = qrCodeService.generatePaymentQRCode(
                            method,
                            request.getCoin().toString(),
                            user.getId().toString(),
                            transactionId);
                    response.setQrCode(qrCode);
                    response.setMessage("Quét mã QR bằng ứng dụng " + method + " để thanh toán");
                    break;
                case "BANK_TRANSFER":
                    response.setBankAccount("123456789");
                    response.setBankName("Ngân hàng ABC");
                    response.setBankOwner("CTY TNHH PLAYERDUO");
                    String transferContent = "NAPTIEN_" + user.getId() + "_" + transactionId;
                    response.setTransferContent(transferContent);
                    response.setMessage("Vui lòng chuyển khoản đúng nội dung để được cộng coin tự động.");
                    break;
                default:
                    response.setMessage("Phương thức thanh toán không hợp lệ!");
                    return ResponseEntity.badRequest().body(response);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setMessage("Lỗi khi tạo mã QR: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/topup-history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TopupHistoryDTO>> getTopupHistory(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        List<Payment> topups = paymentRepository.findByUserIdAndTypeOrderByCreatedAtDesc(user.getId(),
                Payment.PaymentType.TOPUP);
        List<TopupHistoryDTO> result = topups.stream().map(payment -> {
            TopupHistoryDTO dto = new TopupHistoryDTO();
            dto.setId(payment.getId());
            dto.setDateTime(payment.getCreatedAt().toString());
            dto.setCoin(payment.getCoin());
            dto.setMethod(payment.getPaymentMethod());
            dto.setStatus(payment.getStatus());
            switch (payment.getStatus()) {
                case COMPLETED:
                    dto.setStatusText("Thành công");
                    dto.setStatusColor("#4CAF50");
                    break;
                case PENDING:
                    dto.setStatusText("Đang xử lý");
                    dto.setStatusColor("#FFA500");
                    break;
                case FAILED:
                    dto.setStatusText("Thất bại");
                    dto.setStatusColor("#F44336");
                    break;
                default:
                    dto.setStatusText(payment.getStatus().name());
                    dto.setStatusColor("#9E9E9E");
            }
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/topup-users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TopupUserDTO>> getAllTopupUsers() {
        List<Payment> topups = paymentService.getAllTopupPayments();
        List<TopupUserDTO> result = topups.stream().map(payment -> {
            TopupUserDTO dto = new TopupUserDTO();
            dto.setId(payment.getId());
            if (payment.getUser() != null) {
                dto.setFullName(payment.getUser().getFullName());
                dto.setAvatarUrl(payment.getUser().getAvatarUrl());
                dto.setPhoneNumber(payment.getUser().getPhoneNumber());
            }
            dto.setCreatedAt(payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : null);
            dto.setCoin(payment.getCoin());
            dto.setStatus(payment.getStatus());
            dto.setMethod(payment.getPaymentMethod());
            return dto;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/withdraw-users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TopupUserDTO>> getAllWithdrawUsers() {
        List<Payment> withdraws = paymentService.getAllWithdrawPayments();
        List<TopupUserDTO> result = withdraws.stream().map(payment -> {
            TopupUserDTO dto = new TopupUserDTO();
            dto.setId(payment.getId());
            if (payment.getUser() != null) {
                dto.setFullName(payment.getUser().getFullName());
                dto.setAvatarUrl(payment.getUser().getAvatarUrl());
                dto.setPhoneNumber(payment.getUser().getPhoneNumber());
            }
            dto.setCreatedAt(payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : null);
            dto.setCoin(payment.getCoin());
            dto.setStatus(payment.getStatus());
            dto.setMethod(payment.getPaymentMethod());
            return dto;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/vnpay/create")
    public ResponseEntity<?> createVnPayPayment(@RequestParam Long amount, @RequestParam String orderInfo,
            @RequestParam Long userId, HttpServletRequest request) {
        log.info("=== BẮT ĐẦU: Tạo link VNPay ===");
        log.info("Request params: amount={}, orderInfo={}, userId={}", amount, orderInfo, userId);
        log.info("Request URI: {}", request.getRequestURI());
        log.info("Request method: {}", request.getMethod());

        try {
            log.info("Bước 1: Lấy IP address");
            String ipAddr = request.getRemoteAddr();
            log.info("IP address: {}", ipAddr);

            log.info("Bước 2: Tạo txnRef");
            String txnRef = String.valueOf(System.currentTimeMillis());
            log.info("TxnRef: {}", txnRef);

            log.info("Bước 3: Gọi VnPayService.createPaymentUrl");
            String paymentUrl = vnPayService.createPaymentUrl(amount, orderInfo, ipAddr, txnRef);
            log.info("Payment URL tạo thành công: {}", paymentUrl);

            log.info("Bước 4: Tìm user theo userId");
            User user = userService.findById(userId);
            log.info("User tìm thấy: id={}, username={}", user.getId(), user.getUsername());

            log.info("Bước 5: Tạo Payment entity");
            Payment payment = new Payment();
            payment.setUser(user);
            payment.setCoin(amount);
            payment.setCurrency("VND");
            payment.setPaymentMethod(Payment.PaymentMethod.VNPAY);
            payment.setStatus(Payment.PaymentStatus.PENDING);
            payment.setVnpTxnRef(txnRef);
            payment.setDescription(orderInfo);
            payment.setCreatedAt(LocalDateTime.now());
            payment.setType(Payment.PaymentType.TOPUP);
            log.info("Payment entity đã tạo: {}", payment);

            log.info("Bước 6: Lưu Payment vào database");
            Payment savedPayment = paymentRepository.save(payment);
            log.info("Payment đã lưu thành công: id={}", savedPayment.getId());

            log.info("Bước 7: Trả về response");
            Map<String, Object> response = new HashMap<>();
            response.put("paymentUrl", paymentUrl);
            response.put("txnRef", txnRef);
            response.put("paymentId", savedPayment.getId());
            log.info("Response: {}", response);

            log.info("=== KẾT THÚC: Tạo link VNPay thành công ===");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("=== LỖI: Tạo link VNPay thất bại ===");
            log.error("Exception type: {}", e.getClass().getSimpleName());
            log.error("Exception message: {}", e.getMessage());
            log.error("Stack trace:", e);
            return ResponseEntity.badRequest().body("Lỗi tạo link thanh toán: " + e.getMessage());
        }
    }

    @GetMapping("/vnpay-return")
    public ResponseEntity<?> vnpayReturn(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> params.put(k, v[0]));
        boolean valid = vnPayService.verifyVnpayCallback(new HashMap<>(params));
        if (!valid) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Giao dịch không hợp lệ!"));
        }
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        Payment payment = paymentRepository.findByVnpTxnRef(txnRef).orElse(null);
        if (payment == null) return ResponseEntity.ok(Map.of("success", false, "message", "Không tìm thấy đơn hàng!"));
        if ("00".equals(responseCode)) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            paymentRepository.save(payment);
            // Cộng xu cho user
            User user = payment.getUser();
            user.setCoin(user.getCoin() + payment.getCoin());
            userService.save(user);
            return ResponseEntity.ok(Map.of("success", true, "message", "Nạp tiền thành công!"));
        } else {
            return ResponseEntity.ok(Map.of("success", false, "message", "Giao dịch thất bại!"));
        }
    }

    @Transactional
    @PostMapping("/donate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> donate(@RequestBody DonateRequest request, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        User player = userService.findById(request.getPlayerId());
        if (player == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy player nhận donate"));
        }
        if (user.getCoin() < request.getCoin()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Số dư không đủ để donate"));
        }
        // Trừ xu người donate
        user.setCoin(user.getCoin() - request.getCoin());
        userService.save(user);
        // Log số dư người nhận trước khi cộng
        System.out.println("Coin người nhận trước khi cộng: " + player.getCoin());
        // Cộng xu cho player
        player.setCoin(player.getCoin() + request.getCoin());
        System.out.println("Coin người nhận sau khi cộng: " + player.getCoin());
        userService.save(player);

        // Lưu Payment DONATE cho người gửi (trừ xu)
        Payment paymentFrom = new Payment();
        paymentFrom.setUser(user);
        paymentFrom.setPlayer(player);
        paymentFrom.setCoin(request.getCoin());
        paymentFrom.setCurrency("COIN");
        paymentFrom.setStatus(Payment.PaymentStatus.COMPLETED);
        paymentFrom.setPaymentMethod(Payment.PaymentMethod.DONATE);
        paymentFrom.setType(Payment.PaymentType.DONATE);
        paymentFrom.setCreatedAt(java.time.LocalDateTime.now());
        paymentFrom.setDescription("Donate cho " + player.getUsername() + ": " + request.getMessage());
        paymentRepository.save(paymentFrom);

        // Lưu Payment DONATE cho người nhận (cộng xu)
        Payment paymentTo = new Payment();
        paymentTo.setUser(player);
        paymentTo.setPlayer(user);
        paymentTo.setCoin(request.getCoin());
        paymentTo.setCurrency("COIN");
        paymentTo.setStatus(Payment.PaymentStatus.COMPLETED);
        paymentTo.setPaymentMethod(Payment.PaymentMethod.DONATE);
        paymentTo.setType(Payment.PaymentType.DONATE);
        paymentTo.setCreatedAt(java.time.LocalDateTime.now());
        paymentTo.setDescription("Nhận donate từ " + user.getUsername() + ": " + request.getMessage());
        paymentRepository.save(paymentTo);

        return ResponseEntity.ok(Map.of("message", "Donate thành công"));
    }

    @Data
    static class DonateRequest {
        private Long playerId;
        private Long coin;
        private String message;
    }
}

@Data
class PaymentRequest {
    @NotNull(message = "Game player ID is required")
    private Long gamePlayerId;

    @NotNull(message = "Coin is required")
    @Positive(message = "Coin must be positive")
    private Long coin;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;
}

@Data
class ProcessPaymentRequest {
    @NotBlank(message = "Transaction ID is required")
    private String transactionId;
}

@Data
class RefundRequest {
    @NotBlank(message = "Reason is required")
    private String reason;
}

@Data
class TopUpRequest {
    @NotNull
    @Positive
    private Long coin;
}

@Data
class WithdrawRequest {
    @NotNull
    @Positive
    private Long coin;
}

@Data
class DepositRequest {
    @NotNull
    @Positive
    private Long coin;

    @NotBlank
    private String method;
}

@Data
class DepositResponse {
    private String qrCode; // Base64 encoded QR code image
    private String message;
    private String bankAccount;
    private String bankName;
    private String bankOwner;
    private String transferContent;
}

@Data
class TopupHistoryDTO {
    private Long id;
    private String dateTime;
    private Long coin;
    private Payment.PaymentMethod method;
    private Payment.PaymentStatus status;
    private String statusText;
    private String statusColor;
}