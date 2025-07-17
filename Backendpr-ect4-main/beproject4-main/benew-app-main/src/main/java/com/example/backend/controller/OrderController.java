package com.example.backend.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.OrderResponseDTO;
import com.example.backend.dto.OrderSummaryDTO;
import com.example.backend.dto.UserOrderDTO;
import com.example.backend.entity.Order;
import com.example.backend.entity.Payment;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.OrderRepository;
import com.example.backend.repository.PaymentRepository;
import com.example.backend.repository.PlayerReviewRepository;
import com.example.backend.service.OrderService;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:3000")
public class OrderController {

    private final PaymentRepository paymentRepository;
    @Autowired
    private OrderRepository orderRepository;
    private final PlayerReviewRepository playerReviewRepository;
    private final OrderService orderService;

    public OrderController(PaymentRepository paymentRepository, PlayerReviewRepository playerReviewRepository,
            OrderService orderService) {
        this.paymentRepository = paymentRepository;
        this.playerReviewRepository = playerReviewRepository;
        this.orderService = orderService;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderDetails(@PathVariable Long orderId) {
        Payment payment = paymentRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Map<String, Object> data = new HashMap<>();
        data.put("id", payment.getId());
        // Map status FE
        String status;
        if (payment.getStatus() == null || payment.getStatus() == Payment.PaymentStatus.PENDING) {
            status = "PENDING";
        } else if (payment.getStatus() == Payment.PaymentStatus.CONFIRMED) {
            status = "CONFIRMED";
        } else if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
            status = "COMPLETED";
        } else if (payment.getStatus() == Payment.PaymentStatus.CANCELED) {
            status = "REJECTED";
        } else {
            status = payment.getStatus().name();
        }
        data.put("status", status);
        data.put("hirerName", payment.getUser() != null ? payment.getUser().getUsername() : null);
        data.put("hirerId", payment.getUser() != null ? payment.getUser().getId() : null);
        data.put("playerName", payment.getPlayer() != null ? payment.getPlayer().getUsername() : null);
        data.put("playerId", payment.getPlayer() != null ? payment.getPlayer().getId() : null);
        data.put("playerAvatarUrl", payment.getPlayer() != null ? payment.getPlayer().getAvatarUrl() : null);
        data.put("playerRank", payment.getGamePlayer() != null ? payment.getGamePlayer().getRank() : null);
        data.put("game",
                payment.getGamePlayer() != null && payment.getGamePlayer().getGame() != null
                        ? payment.getGamePlayer().getGame().getName()
                        : null);
        data.put("specialRequest", payment.getDescription());
        long hours = 0;
        if (payment.getStartTime() != null && payment.getEndTime() != null) {
            hours = java.time.Duration.between(payment.getStartTime(), payment.getEndTime()).toHours();
        }
        data.put("hours", hours);
        data.put("totalCoin", payment.getCoin());
        data.put("startTime", payment.getStartTime());

        return ResponseEntity.ok(data);
    }

    // API MỚI: Lấy chi tiết đơn thuê dùng entity Order nhưng trả về dữ liệu y hệt
    // API cũ
    @GetMapping("/detail/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderDetailNew(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Map<String, Object> data = new HashMap<>();
        data.put("id", order.getId());

        // Map status FE - giống hệt API cũ
        String status;
        if (order.getStatus() == null || "PENDING".equalsIgnoreCase(order.getStatus())) {
            status = "PENDING";
        } else if ("CONFIRMED".equalsIgnoreCase(order.getStatus())) {
            status = "CONFIRMED";
        } else if ("COMPLETED".equalsIgnoreCase(order.getStatus())) {
            status = "COMPLETED";
        } else if ("CANCELED".equalsIgnoreCase(order.getStatus())) {
            status = "REJECTED";
        } else {
            status = order.getStatus();
        }
        data.put("status", status);

        // Thông tin người thuê (hirer) - giống hệt API cũ
        data.put("hirerName", order.getRenter() != null ? order.getRenter().getUsername() : null);
        data.put("hirerId", order.getRenter() != null ? order.getRenter().getId() : null);

        // Thông tin player - giống hệt API cũ
        data.put("playerName", order.getPlayer() != null ? order.getPlayer().getUsername() : null);
        data.put("playerId",
                order.getPlayer() != null && order.getPlayer().getUser() != null ? order.getPlayer().getUser().getId()
                        : null);
        data.put("playerAvatarUrl",
                order.getPlayer() != null && order.getPlayer().getUser() != null
                        ? order.getPlayer().getUser().getAvatarUrl()
                        : null);

        // Thông tin game và rank - giống hệt API cũ
        data.put("playerRank", order.getPlayer() != null ? order.getPlayer().getRank() : null);
        data.put("game",
                order.getPlayer() != null && order.getPlayer().getGame() != null ? order.getPlayer().getGame().getName()
                        : null);

        // Yêu cầu đặc biệt - giống hệt API cũ (Order không có trường này, để null)
        data.put("specialRequest", null);

        // Tính số giờ thuê - giống hệt API cũ
        long hours = 0;
        if (order.getStartTime() != null && order.getEndTime() != null) {
            hours = java.time.Duration.between(order.getStartTime(), order.getEndTime()).toHours();
        }
        data.put("hours", hours);

        // Thông tin giá - giống hệt API cũ
        data.put("totalCoin", order.getPrice());
        data.put("startTime", order.getStartTime());

        return ResponseEntity.ok(data);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        List<OrderResponseDTO> result = orders.stream().map(order -> new OrderResponseDTO(
                order.getId(),
                order.getRenter() != null ? order.getRenter().getUsername() : "",
                order.getPlayer() != null ? order.getPlayer().getUsername() : "",
                order.getStartTime() != null && order.getEndTime() != null
                        ? order.getStartTime().format(formatter) + " - " + order.getEndTime().format(formatter)
                        : "",
                order.getStatus() != null ? order.getStatus() : "",
                order.getPrice())).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByUser(@PathVariable Long userId) {
        List<Order> orders = orderRepository.findAll().stream()
                .filter(order -> order.getRenter() != null && order.getRenter().getId().equals(userId))
                .toList();
        return ResponseEntity.ok(mapOrdersToDTO(orders));
    }

    @GetMapping("/player/{playerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByPlayer(@PathVariable Long playerId) {
        List<Order> orders = orderRepository.findAll().stream()
                .filter(order -> order.getPlayer() != null && order.getPlayer().getId().equals(playerId))
                .toList();
        return ResponseEntity.ok(mapOrdersToDTO(orders));
    }

    @GetMapping("/user-all/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserOrderDTO>> getAllUserOrders(@PathVariable Long userId) {
        List<Order> allOrders = orderRepository.findAll();

        // Lấy đơn hàng user thuê (là renter)
        List<Order> hiredOrders = allOrders.stream()
                .filter(order -> order.getRenter() != null && order.getRenter().getId().equals(userId))
                .toList();

        // Lấy đơn hàng user được thuê (là player)
        List<Order> hiringOrders = allOrders.stream()
                .filter(order -> order.getPlayer() != null && order.getPlayer().getUser() != null &&
                        order.getPlayer().getUser().getId().equals(userId))
                .toList();

        List<UserOrderDTO> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

        // Xử lý đơn hàng thuê (HIRED)
        for (Order order : hiredOrders) {
            UserOrderDTO dto = new UserOrderDTO();
            dto.setId(order.getId());
            dto.setRenterName(order.getRenter() != null ? order.getRenter().getUsername() : "");
            dto.setPlayerName(order.getPlayer() != null ? order.getPlayer().getUsername() : "");
            dto.setOrderType("HIRED"); // Đơn thuê
            dto.setGame(order.getPlayer() != null && order.getPlayer().getGame() != null
                    ? order.getPlayer().getGame().getName()
                    : "");
            dto.setPlayerRank(order.getPlayer() != null ? order.getPlayer().getRank() : "");
            dto.setPlayerAvatarUrl(order.getPlayer() != null && order.getPlayer().getUser() != null
                    ? order.getPlayer().getUser().getAvatarUrl()
                    : "");
            dto.setRenterAvatarUrl(order.getRenter() != null ? order.getRenter().getAvatarUrl() : "");

            // Tính thời gian thuê
            if (order.getStartTime() != null && order.getEndTime() != null) {
                dto.setHireTime(order.getStartTime().format(formatter) + " - " + order.getEndTime().format(formatter));
                dto.setHours(java.time.Duration.between(order.getStartTime(), order.getEndTime()).toHours());
            } else {
                dto.setHireTime("");
                dto.setHours(0L);
            }

            dto.setStatus(order.getStatus() != null ? order.getStatus() : "");
            dto.setPrice(order.getPrice());
            dto.setStatusLabel(getStatusLabel(order.getStatus()));

            result.add(dto);
        }

        // Xử lý đơn hàng được thuê (HIRING)
        for (Order order : hiringOrders) {
            UserOrderDTO dto = new UserOrderDTO();
            dto.setId(order.getId());
            dto.setRenterName(order.getRenter() != null ? order.getRenter().getUsername() : "");
            dto.setPlayerName(order.getPlayer() != null ? order.getPlayer().getUsername() : "");
            dto.setOrderType("HIRING"); // Đơn được thuê
            dto.setGame(order.getPlayer() != null && order.getPlayer().getGame() != null
                    ? order.getPlayer().getGame().getName()
                    : "");
            dto.setPlayerRank(order.getPlayer() != null ? order.getPlayer().getRank() : "");
            dto.setPlayerAvatarUrl(order.getPlayer() != null && order.getPlayer().getUser() != null
                    ? order.getPlayer().getUser().getAvatarUrl()
                    : "");
            dto.setRenterAvatarUrl(order.getRenter() != null ? order.getRenter().getAvatarUrl() : "");

            // Tính thời gian thuê
            if (order.getStartTime() != null && order.getEndTime() != null) {
                dto.setHireTime(order.getStartTime().format(formatter) + " - " + order.getEndTime().format(formatter));
                dto.setHours(java.time.Duration.between(order.getStartTime(), order.getEndTime()).toHours());
            } else {
                dto.setHireTime("");
                dto.setHours(0L);
            }

            dto.setStatus(order.getStatus() != null ? order.getStatus() : "");
            dto.setPrice(order.getPrice());
            dto.setStatusLabel(getStatusLabel(order.getStatus()));

            result.add(dto);
        }

        // Sắp xếp theo thời gian tạo (mới nhất trước)
        result.sort((o1, o2) -> {
            // Tìm order tương ứng để so sánh thời gian
            Order order1 = allOrders.stream().filter(o -> o.getId().equals(o1.getId())).findFirst().orElse(null);
            Order order2 = allOrders.stream().filter(o -> o.getId().equals(o2.getId())).findFirst().orElse(null);

            if (order1 != null && order2 != null && order1.getStartTime() != null && order2.getStartTime() != null) {
                return order2.getStartTime().compareTo(order1.getStartTime());
            }
            return 0;
        });

        return ResponseEntity.ok(result);
    }

    @GetMapping("/user-stats/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getUserOrderStats(@PathVariable Long userId) {
        List<Order> allOrders = orderRepository.findAll();

        // Lấy đơn hàng user thuê (là renter)
        List<Order> hiredOrders = allOrders.stream()
                .filter(order -> order.getRenter() != null && order.getRenter().getId().equals(userId))
                .toList();

        // Lấy đơn hàng user được thuê (là player)
        List<Order> hiringOrders = allOrders.stream()
                .filter(order -> order.getPlayer() != null && order.getPlayer().getUser() != null &&
                        order.getPlayer().getUser().getId().equals(userId))
                .toList();

        // Thống kê đơn thuê
        long totalHired = hiredOrders.size();
        long completedHired = hiredOrders.stream().filter(o -> "COMPLETED".equalsIgnoreCase(o.getStatus())).count();
        long pendingHired = hiredOrders.stream().filter(o -> "PENDING".equalsIgnoreCase(o.getStatus())).count();
        long confirmedHired = hiredOrders.stream().filter(o -> "CONFIRMED".equalsIgnoreCase(o.getStatus())).count();
        long canceledHired = hiredOrders.stream().filter(o -> "CANCELED".equalsIgnoreCase(o.getStatus())).count();
        long totalSpent = hiredOrders.stream().filter(o -> "COMPLETED".equalsIgnoreCase(o.getStatus()))
                .mapToLong(o -> o.getPrice() != null ? o.getPrice() : 0L).sum();

        // Thống kê đơn được thuê
        long totalHiring = hiringOrders.size();
        long completedHiring = hiringOrders.stream().filter(o -> "COMPLETED".equalsIgnoreCase(o.getStatus())).count();
        long pendingHiring = hiringOrders.stream().filter(o -> "PENDING".equalsIgnoreCase(o.getStatus())).count();
        long confirmedHiring = hiringOrders.stream().filter(o -> "CONFIRMED".equalsIgnoreCase(o.getStatus())).count();
        long canceledHiring = hiringOrders.stream().filter(o -> "CANCELED".equalsIgnoreCase(o.getStatus())).count();
        long totalEarned = hiringOrders.stream().filter(o -> "COMPLETED".equalsIgnoreCase(o.getStatus()))
                .mapToLong(o -> o.getPrice() != null ? o.getPrice() : 0L).sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", totalHired + totalHiring);
        stats.put("hiredOrders", Map.of(
                "total", totalHired,
                "completed", completedHired,
                "pending", pendingHired,
                "confirmed", confirmedHired,
                "canceled", canceledHired,
                "totalSpent", totalSpent));
        stats.put("hiringOrders", Map.of(
                "total", totalHiring,
                "completed", completedHiring,
                "pending", pendingHiring,
                "confirmed", confirmedHiring,
                "canceled", canceledHiring,
                "totalEarned", totalEarned));

        return ResponseEntity.ok(stats);
    }

    @PostMapping("/send-review-reminders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sendReviewReminders() {
        try {
            // Gọi method từ OrderService để gửi thông báo đánh giá
            orderService.autoCompleteOrdersAndNotifyReview();
            return ResponseEntity
                    .ok("Đã tự động hoàn thành đơn hàng và gửi thông báo đánh giá cho các đơn hàng đã kết thúc");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi gửi thông báo: " + e.getMessage());
        }
    }

    @GetMapping("/expired-orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getExpiredOrders() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Order> expiredOrders = orderRepository.findAll().stream()
                    .filter(o -> "CONFIRMED".equalsIgnoreCase(o.getStatus()))
                    .filter(o -> o.getEndTime() != null && o.getEndTime().isBefore(now))
                    .toList();

            List<Map<String, Object>> result = new ArrayList<>();
            for (Order order : expiredOrders) {
                Map<String, Object> orderInfo = new HashMap<>();
                orderInfo.put("id", order.getId());
                orderInfo.put("renterName", order.getRenter() != null ? order.getRenter().getUsername() : "");
                orderInfo.put("playerName", order.getPlayer() != null ? order.getPlayer().getUsername() : "");
                orderInfo.put("startTime", order.getStartTime());
                orderInfo.put("endTime", order.getEndTime());
                orderInfo.put("price", order.getPrice());
                orderInfo.put("hoursExpired",
                        order.getEndTime() != null ? java.time.Duration.between(order.getEndTime(), now).toHours() : 0);
                result.add(orderInfo);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi lấy danh sách đơn hàng hết hạn: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByStatus(@RequestParam String status) {
        List<Order> orders = orderRepository.findAll().stream()
                .filter(order -> order.getStatus() != null && order.getStatus().equalsIgnoreCase(status))
                .toList();
        return ResponseEntity.ok(mapOrdersToDTO(orders));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getOrderStats(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long playerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        List<Order> orders = orderRepository.findAll();
        if (userId != null) {
            orders = orders.stream().filter(o -> o.getRenter() != null && o.getRenter().getId().equals(userId))
                    .toList();
        }
        if (playerId != null) {
            orders = orders.stream().filter(o -> o.getPlayer() != null && o.getPlayer().getId().equals(playerId))
                    .toList();
        }
        if (from != null) {
            orders = orders.stream().filter(o -> o.getStartTime() != null && !o.getStartTime().isBefore(from)).toList();
        }
        if (to != null) {
            orders = orders.stream().filter(o -> o.getEndTime() != null && !o.getEndTime().isAfter(to)).toList();
        }
        long totalOrders = orders.size();
        long completedOrders = orders.stream().filter(o -> "COMPLETED".equalsIgnoreCase(o.getStatus())).count();
        long canceledOrders = orders.stream().filter(o -> "CANCELED".equalsIgnoreCase(o.getStatus())).count();
        long confirmedOrders = orders.stream().filter(o -> "CONFIRMED".equalsIgnoreCase(o.getStatus())).count();
        long pendingOrders = orders.stream().filter(o -> "PENDING".equalsIgnoreCase(o.getStatus())).count();
        long totalRevenue = orders.stream().filter(o -> "COMPLETED".equalsIgnoreCase(o.getStatus()))
                .mapToLong(o -> o.getPrice() != null ? o.getPrice() : 0L).sum();
        Map<String, Object> stats = Map.of(
                "totalOrders", totalOrders,
                "completedOrders", completedOrders,
                "canceledOrders", canceledOrders,
                "confirmedOrders", confirmedOrders,
                "pendingOrders", pendingOrders,
                "totalRevenue", totalRevenue);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/summary")
    public ResponseEntity<List<OrderSummaryDTO>> getOrderSummary() {
        List<Order> orders = orderRepository.findAll();
        List<OrderSummaryDTO> result = new ArrayList<>();
        java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (Order order : orders) {
            OrderSummaryDTO dto = new OrderSummaryDTO();
            dto.setId(order.getId());
            dto.setRenterName(order.getRenter() != null ? order.getRenter().getUsername() : "");
            dto.setPlayerName(order.getPlayer() != null ? order.getPlayer().getUsername() : "");
            if (order.getStartTime() != null && order.getEndTime() != null) {
                String start = order.getStartTime().format(timeFormatter);
                String end = order.getEndTime().format(timeFormatter);
                long minutes = java.time.Duration.between(order.getStartTime(), order.getEndTime()).toMinutes();
                long hours = minutes / 60;
                long mins = minutes % 60;
                String duration = (hours > 0 ? hours + " giờ " : "") + (mins > 0 ? mins + " phút" : "");
                dto.setTimeRange(start + " -> " + end + (duration.isEmpty() ? "" : " (" + duration.trim() + ")"));
                dto.setDate(order.getStartTime().format(dateFormatter));
            } else {
                dto.setTimeRange("");
                dto.setDate("");
            }
            dto.setPrice(order.getPrice());
            // Mapping trạng thái
            String statusLabel;
            String status = order.getStatus();
            if (status == null || "PENDING".equalsIgnoreCase(status)) {
                statusLabel = "Chờ xác nhận";
            } else if ("CONFIRMED".equalsIgnoreCase(status)) {
                statusLabel = "Đã xác nhận";
            } else if ("COMPLETED".equalsIgnoreCase(status)) {
                statusLabel = "Đã hoàn thành";
            } else if ("CANCELED".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status)) {
                statusLabel = "Bị hủy";
            } else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
                statusLabel = "Đang diễn ra";
            } else {
                statusLabel = status;
            }
            dto.setStatusLabel(statusLabel);
            dto.setRenterAvatarUrl(order.getRenter() != null ? order.getRenter().getAvatarUrl() : "");
            dto.setPlayerAvatarUrl(order.getPlayer() != null && order.getPlayer().getUser() != null
                    ? order.getPlayer().getUser().getAvatarUrl()
                    : "");
            result.add(dto);
        }
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> deleteOrder(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!"COMPLETED".equalsIgnoreCase(order.getStatus())) {
            return ResponseEntity.badRequest().body("Chỉ có thể xóa đơn đã hoàn thành (COMPLETED)");
        }
        orderRepository.delete(order);
        return ResponseEntity.ok("Order deleted successfully");
    }

    private List<OrderResponseDTO> mapOrdersToDTO(List<Order> orders) {
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        return orders.stream().map(order -> new OrderResponseDTO(
                order.getId(),
                order.getRenter() != null ? order.getRenter().getUsername() : "",
                order.getPlayer() != null ? order.getPlayer().getUsername() : "",
                order.getStartTime() != null && order.getEndTime() != null
                        ? order.getStartTime().format(formatter) + " - " + order.getEndTime().format(formatter)
                        : "",
                order.getStatus() != null ? order.getStatus() : "",
                order.getPrice())).toList();
    }

    private String getStatusLabel(String status) {
        if (status == null || "PENDING".equalsIgnoreCase(status)) {
            return "Chờ xác nhận";
        } else if ("CONFIRMED".equalsIgnoreCase(status)) {
            return "Đã xác nhận";
        } else if ("COMPLETED".equalsIgnoreCase(status)) {
            return "Đã hoàn thành";
        } else if ("CANCELED".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status)) {
            return "Bị hủy";
        } else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
            return "Đang diễn ra";
        } else {
            return status;
        }
    }
}