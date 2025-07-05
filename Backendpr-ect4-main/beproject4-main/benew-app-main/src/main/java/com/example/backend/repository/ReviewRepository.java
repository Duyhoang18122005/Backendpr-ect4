package com.example.backend.repository;

import com.example.backend.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByGamePlayerId(Long gamePlayerId);
    List<Review> findByUserId(Long userId);
    List<Review> findByStatus(String status);
    List<Review> findByGamePlayerIdAndStatus(Long gamePlayerId, String status);
    Double findAverageRatingByGamePlayerId(Long gamePlayerId);
    List<Review> findByGamePlayerIdAndUserId(Long gamePlayerId, Long userId);
} 