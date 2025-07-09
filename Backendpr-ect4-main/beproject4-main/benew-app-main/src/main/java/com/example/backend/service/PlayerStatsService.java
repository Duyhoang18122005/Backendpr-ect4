package com.example.backend.service;

import com.example.backend.dto.PlayerStatsDTO;
import com.example.backend.dto.ReviewDTO;
import com.example.backend.dto.HireStatsDTO;
import com.example.backend.entity.Payment;
import com.example.backend.entity.PlayerReview;
import com.example.backend.entity.User;
import com.example.backend.entity.GamePlayer;
import com.example.backend.repository.PaymentRepository;
import com.example.backend.repository.PlayerReviewRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.GamePlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlayerStatsService {
    private final PaymentRepository paymentRepository;
    private final PlayerReviewRepository playerReviewRepository;
    private final UserRepository userRepository;
    private final GamePlayerRepository gamePlayerRepository;

    public PlayerStatsService(PaymentRepository paymentRepository,
                            PlayerReviewRepository playerReviewRepository,
                            UserRepository userRepository,
                            GamePlayerRepository gamePlayerRepository) {
        this.paymentRepository = paymentRepository;
        this.playerReviewRepository = playerReviewRepository;
        this.userRepository = userRepository;
        this.gamePlayerRepository = gamePlayerRepository;
    }

    @Transactional(readOnly = true)
    public PlayerStatsDTO getPlayerStats(Long playerId) {
        User player = userRepository.findById(playerId)
            .orElseThrow(() -> new RuntimeException("Player not found"));

        // Lấy tất cả các lượt thuê của player
        List<Payment> hires = paymentRepository.findByPlayerIdAndTypeOrderByCreatedAtDesc(playerId, Payment.PaymentType.HIRE);
        
        // Lấy tất cả đánh giá của player
        List<PlayerReview> reviews = playerReviewRepository.findByGamePlayerUserId(playerId);

        PlayerStatsDTO stats = new PlayerStatsDTO();
        stats.setPlayerId(playerId);
        
        // Lấy tên hiển thị từ GamePlayer thay vì User
        List<GamePlayer> gamePlayers = gamePlayerRepository.findByUserId(playerId);
        if (!gamePlayers.isEmpty()) {
            // Lấy tên hiển thị từ GamePlayer đầu tiên (thường chỉ có 1 player per user)
            stats.setPlayerName(gamePlayers.get(0).getUsername());
        } else {
            // Fallback về username của user nếu không tìm thấy GamePlayer
            stats.setPlayerName(player.getUsername());
        }

        // Tính toán thống kê cơ bản
        stats.setTotalHires(hires.size());
        stats.setCompletedHires((int) hires.stream()
            .filter(h -> Payment.PaymentStatus.COMPLETED.equals(h.getStatus()))
            .count());
        stats.setTotalHireHours(calculateTotalHours(hires));
        stats.setCompletionRate(calculateCompletionRate(hires));
        stats.setTotalEarnings(calculateTotalEarnings(hires));

        // Tính toán rating
        Double averageRating = playerReviewRepository.getAverageRatingByPlayerId(playerId);
        stats.setAverageRating(averageRating != null ? averageRating : 0.0);
        stats.setTotalReviews(reviews.size());

        // Lấy 5 đánh giá gần nhất
        stats.setRecentReviews(getRecentReviews(reviews));

        // Tính toán thống kê theo thời gian
        stats.setHireStats(getHireStatsByPeriod(hires));

        return stats;
    }

    private Integer calculateTotalHours(List<Payment> hires) {
        return hires.stream()
            .filter(h -> h.getStartTime() != null && h.getEndTime() != null)
            .mapToInt(h -> {
                long hours = java.time.Duration.between(h.getStartTime(), h.getEndTime()).toHours();
                return (int) hours;
            })
            .sum();
    }

    private Double calculateCompletionRate(List<Payment> hires) {
        if (hires.isEmpty()) return 0.0;
        long completed = hires.stream()
            .filter(h -> Payment.PaymentStatus.COMPLETED.equals(h.getStatus()))
            .count();
        return (double) completed / hires.size() * 100;
    }

    private Long calculateTotalEarnings(List<Payment> hires) {
        return hires.stream()
            .filter(h -> Payment.PaymentStatus.COMPLETED.equals(h.getStatus()))
            .mapToLong(Payment::getCoin)
            .sum();
    }

    private List<ReviewDTO> getRecentReviews(List<PlayerReview> reviews) {
        return reviews.stream()
            .sorted(Comparator.comparing(PlayerReview::getCreatedAt).reversed())
            .limit(5)
            .map(review -> {
                ReviewDTO dto = new ReviewDTO();
                dto.setReviewId(review.getId());
                dto.setRating(review.getRating());
                dto.setComment(review.getComment());
                dto.setReviewerName(review.getUser().getUsername());
                dto.setCreatedAt(review.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                return dto;
            })
            .collect(Collectors.toList());
    }

    private List<HireStatsDTO> getHireStatsByPeriod(List<Payment> hires) {
        Map<String, HireStatsDTO> statsMap = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (Payment hire : hires) {
            String period = hire.getCreatedAt().format(formatter);
            HireStatsDTO stats = statsMap.computeIfAbsent(period, k -> {
                HireStatsDTO dto = new HireStatsDTO();
                dto.setPeriod(k);
                dto.setTotalHires(0);
                dto.setCompletedHires(0);
                dto.setTotalHours(0);
                dto.setEarnings(0L);
                return dto;
            });

            stats.setTotalHires(stats.getTotalHires() + 1);
            if (Payment.PaymentStatus.COMPLETED.equals(hire.getStatus())) {
                stats.setCompletedHires(stats.getCompletedHires() + 1);
                if (hire.getStartTime() != null && hire.getEndTime() != null) {
                    long hours = java.time.Duration.between(hire.getStartTime(), hire.getEndTime()).toHours();
                    stats.setTotalHours(stats.getTotalHours() + (int) hours);
                }
                stats.setEarnings(stats.getEarnings() + hire.getCoin());
            }
        }

        return new ArrayList<>(statsMap.values());
    }
} 