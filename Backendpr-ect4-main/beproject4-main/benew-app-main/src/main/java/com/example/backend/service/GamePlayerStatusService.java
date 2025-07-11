package com.example.backend.service;

import com.example.backend.entity.GamePlayer;
import com.example.backend.entity.Payment;
import com.example.backend.repository.GamePlayerRepository;
import com.example.backend.repository.PaymentRepository;
import com.example.backend.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GamePlayerStatusService {
    private final GamePlayerRepository gamePlayerRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;

    public GamePlayerStatusService(GamePlayerRepository gamePlayerRepository,
                                 PaymentRepository paymentRepository,
                                 NotificationService notificationService) {
        this.gamePlayerRepository = gamePlayerRepository;
        this.paymentRepository = paymentRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedRate = 60000) // Chạy mỗi phút
    @Transactional
    public void updateExpiredHires() {
        LocalDateTime now = LocalDateTime.now();
        // Tìm tất cả các payment đang CONFIRMED và đã hết hạn
        List<Payment> expiredPayments = paymentRepository.findByStatusAndEndTimeBefore(
            Payment.PaymentStatus.CONFIRMED, now);

        for (Payment payment : expiredPayments) {
            // Cập nhật trạng thái payment
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            paymentRepository.save(payment);

            // Cập nhật trạng thái game player
            GamePlayer gamePlayer = payment.getGamePlayer();
            if (gamePlayer != null) {
                gamePlayer.setStatus("AVAILABLE");
                gamePlayer.setHiredBy(null);
                gamePlayer.setHireDate(null);
                gamePlayer.setReturnDate(null);
                gamePlayer.setHoursHired(null);
                gamePlayerRepository.save(gamePlayer);
            }

            // Gửi notification nhắc đánh giá cho người thuê
            notificationService.createNotification(
                payment.getUser().getId(),
                "Đánh giá player sau khi thuê",
                "Bạn vừa hoàn thành hợp đồng thuê với player " + (gamePlayer != null ? gamePlayer.getUsername() : "") + ". Hãy để lại đánh giá của bạn!",
                "review_reminder",
                null,
                payment.getId().toString()
            );
        }
    }
} 