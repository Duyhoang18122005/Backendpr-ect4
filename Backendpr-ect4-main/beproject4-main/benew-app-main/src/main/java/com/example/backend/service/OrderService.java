package com.example.backend.service;

import com.example.backend.entity.Order;
import com.example.backend.repository.OrderRepository;
import com.example.backend.service.NotificationService;
import com.example.backend.service.GamePlayerService;
import com.example.backend.entity.Payment;
import com.example.backend.repository.PaymentRepository;
import com.example.backend.repository.PlayerReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.backend.service.PlayerRewardService;

@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private PlayerReviewRepository playerReviewRepository;
    @Autowired
    private GamePlayerService gamePlayerService;
    @Autowired
    private PlayerRewardService playerRewardService;

    // Chạy mỗi 5 phút
    @Scheduled(fixedRate = 300000)
    public void notifyUpcomingOrders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime soon = now.plusMinutes(15);
        List<Order> upcoming = orderRepository.findAll().stream()
            .filter(o -> "CONFIRMED".equalsIgnoreCase(o.getStatus()))
            .filter(o -> o.getStartTime() != null && o.getStartTime().isAfter(now) && o.getStartTime().isBefore(soon))
            .toList();
        for (Order order : upcoming) {
            if (order.getRenter() != null) {
                notificationService.createNotification(
                    order.getRenter().getId(),
                    "Sắp đến giờ thuê!",
                    "Đơn thuê của bạn với player " + (order.getPlayer() != null ? order.getPlayer().getUsername() : "") + " sẽ bắt đầu lúc " + order.getStartTime() + ".",
                    "order_upcoming",
                    null,
                    order.getId().toString()
                );
            }
            if (order.getPlayer() != null && order.getPlayer().getUser() != null) {
                notificationService.createNotification(
                    order.getPlayer().getUser().getId(),
                    "Sắp đến giờ thuê!",
                    "Bạn sắp có đơn thuê với người dùng " + (order.getRenter() != null ? order.getRenter().getUsername() : "") + " lúc " + order.getStartTime() + ".",
                    "order_upcoming",
                    null,
                    order.getId().toString()
                );
            }
        }
        // Nhắc nhở khi sắp kết thúc
        List<Order> ending = orderRepository.findAll().stream()
            .filter(o -> "CONFIRMED".equalsIgnoreCase(o.getStatus()))
            .filter(o -> o.getEndTime() != null && o.getEndTime().isAfter(now) && o.getEndTime().isBefore(soon))
            .toList();
        for (Order order : ending) {
            if (order.getRenter() != null) {
                notificationService.createNotification(
                    order.getRenter().getId(),
                    "Đơn thuê sắp kết thúc!",
                    "Đơn thuê của bạn với player " + (order.getPlayer() != null ? order.getPlayer().getUsername() : "") + " sẽ kết thúc lúc " + order.getEndTime() + ".",
                    "order_ending",
                    null,
                    order.getId().toString()
                );
            }
            if (order.getPlayer() != null && order.getPlayer().getUser() != null) {
                notificationService.createNotification(
                    order.getPlayer().getUser().getId(),
                    "Đơn thuê sắp kết thúc!",
                    "Bạn sắp kết thúc đơn thuê với người dùng " + (order.getRenter() != null ? order.getRenter().getUsername() : "") + " lúc " + order.getEndTime() + ".",
                    "order_ending",
                    null,
                    order.getId().toString()
                );
            }
        }
    }

    // Chạy mỗi 10 phút để kiểm tra đơn hàng đã kết thúc, tự động hoàn thành và gửi thông báo đánh giá
    @Scheduled(fixedRate = 600000)
    @Transactional
    public void autoCompleteOrdersAndNotifyReview() {
        LocalDateTime now = LocalDateTime.now();
        logger.info("Checking for orders that need to be auto-completed and review notifications...");
        
        // Lấy tất cả đơn hàng đã kết thúc (endTime < now) và có trạng thái CONFIRMED
        List<Order> expiredOrders = orderRepository.findAll().stream()
            .filter(o -> "CONFIRMED".equalsIgnoreCase(o.getStatus()))
            .filter(o -> o.getEndTime() != null && o.getEndTime().isBefore(now))
            .toList();
        
        logger.info("Found {} expired orders to auto-complete", expiredOrders.size());
        
        for (Order order : expiredOrders) {
            try {
                // Tự động hoàn thành đơn hàng
                order.setStatus("COMPLETED");
                orderRepository.save(order);

                // Gọi logic thưởng cho player
                playerRewardService.processOrderCompleted(order);
                
                // Cập nhật trạng thái player về AVAILABLE
                if (order.getPlayer() != null) {
                    order.getPlayer().setStatus("AVAILABLE");
                    order.getPlayer().setHiredBy(null);
                    order.getPlayer().setHireDate(null);
                    order.getPlayer().setReturnDate(null);
                    order.getPlayer().setHoursHired(null);
                    gamePlayerService.save(order.getPlayer());
                }
                
                logger.info("Auto-completed order {} at {}", order.getId(), now);
                
                // Gửi thông báo hoàn thành cho cả user và player
                if (order.getRenter() != null) {
                    notificationService.createNotification(
                        order.getRenter().getId(),
                        "Đơn thuê đã tự động hoàn thành",
                        "Đơn thuê của bạn đã được tự động hoàn thành do hết thời gian.",
                        "order_auto_complete",
                        null,
                        order.getId().toString()
                    );
                }
                
                if (order.getPlayer() != null && order.getPlayer().getUser() != null) {
                    notificationService.createNotification(
                        order.getPlayer().getUser().getId(),
                        "Đơn thuê đã tự động hoàn thành",
                        "Đơn thuê của bạn đã được tự động hoàn thành do hết thời gian.",
                        "order_auto_complete",
                        null,
                        order.getId().toString()
                    );
                }
                
                // Kiểm tra xem đã có đánh giá cho đơn hàng này chưa
                boolean hasReview = playerReviewRepository.existsByOrderId(order.getId());
                
                if (!hasReview) {
                    // Gửi thông báo đánh giá cho người thuê
                    if (order.getRenter() != null) {
                        String playerName = order.getPlayer() != null ? order.getPlayer().getUsername() : "Player";
                        
                        notificationService.createNotification(
                            order.getRenter().getId(),
                            "Đánh giá player sau khi thuê",
                            "Đơn thuê với " + playerName + " đã kết thúc. Hãy đánh giá trải nghiệm của bạn!",
                            "order_review_reminder",
                            null,
                            order.getId().toString()
                        );
                        
                        logger.info("Sent review reminder notification to user {} for auto-completed order {}", 
                                  order.getRenter().getUsername(), order.getId());
                    }
                } else {
                    logger.info("Order {} already has review, skipping review notification", order.getId());
                }
                
            } catch (Exception e) {
                logger.error("Error auto-completing order {}: {}", order.getId(), e.getMessage());
            }
        }
    }

    // Method để gửi thông báo đánh giá ngay khi đơn hàng được hoàn thành
    @Transactional
    public void sendReviewNotificationForCompletedOrder(Order order) {
        try {
            // Kiểm tra xem đã có đánh giá cho đơn hàng này chưa
            boolean hasReview = playerReviewRepository.existsByOrderId(order.getId());
            
            if (!hasReview && order.getRenter() != null) {
                String playerName = order.getPlayer() != null ? order.getPlayer().getUsername() : "Player";
                
                notificationService.createNotification(
                    order.getRenter().getId(),
                    "Đánh giá player sau khi thuê",
                    "Đơn thuê với " + playerName + " đã hoàn thành. Hãy đánh giá trải nghiệm của bạn!",
                    "order_review_reminder",
                    null,
                    order.getId().toString()
                );
                
                logger.info("Sent immediate review notification to user {} for completed order {}", 
                          order.getRenter().getUsername(), order.getId());
            }
        } catch (Exception e) {
            logger.error("Error sending immediate review notification for order {}: {}", order.getId(), e.getMessage());
        }
    }

    @Transactional
    public void migratePaymentsToOrders() {
        List<Payment> payments = paymentRepository.findAll().stream()
            .filter(p -> p.getType() != null && p.getType().name().equals("HIRE"))
            .toList();
        for (Payment payment : payments) {
            Order order = new Order();
            order.setRenter(payment.getUser());
            order.setPlayer(payment.getGamePlayer());
            order.setStartTime(payment.getStartTime());
            order.setEndTime(payment.getEndTime());
            order.setPrice(payment.getCoin());
            order.setStatus(payment.getStatus() != null ? payment.getStatus().name() : "");
            orderRepository.save(order);
        }
    }
} 