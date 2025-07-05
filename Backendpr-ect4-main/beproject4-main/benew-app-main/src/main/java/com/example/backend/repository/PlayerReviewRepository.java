package com.example.backend.repository;

import com.example.backend.entity.PlayerReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PlayerReviewRepository extends JpaRepository<PlayerReview, Long> {
    @Query("SELECT AVG(r.rating) FROM PlayerReview r WHERE r.gamePlayer.user.id = ?1")
    Double getAverageRatingByPlayerId(Long playerId);
    
    boolean existsByOrderId(Long orderId);
    
    PlayerReview findByOrderId(Long orderId);
    
    // Đánh giá mà player nhận được (player là người được đánh giá)
    List<PlayerReview> findByGamePlayerUserId(Long playerUserId);
    
    // Đánh giá mà user đã viết (user là người đánh giá)
    List<PlayerReview> findByUserId(Long reviewerUserId);
    
    // Đánh giá theo gamePlayer ID (không phải user ID)
    List<PlayerReview> findByGamePlayerId(Long gamePlayerId);
    
    @Query("SELECT AVG(r.rating) FROM PlayerReview r WHERE r.gamePlayer.id = ?1")
    Double getAverageRatingByGamePlayerId(Long gamePlayerId);
} 