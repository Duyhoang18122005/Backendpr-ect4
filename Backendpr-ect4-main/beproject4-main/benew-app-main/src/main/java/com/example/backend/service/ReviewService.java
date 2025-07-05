package com.example.backend.service;

import com.example.backend.entity.Review;
import com.example.backend.entity.GamePlayer;
import com.example.backend.entity.User;
import com.example.backend.repository.ReviewRepository;
import com.example.backend.repository.GamePlayerRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.ReviewException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository,
                        GamePlayerRepository gamePlayerRepository,
                        UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.gamePlayerRepository = gamePlayerRepository;
        this.userRepository = userRepository;
    }

    public Review createReview(Long gamePlayerId, Long userId, Double rating, String comment) {
        GamePlayer gamePlayer = gamePlayerRepository.findById(gamePlayerId)
                .orElseThrow(() -> new ResourceNotFoundException("Game player not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        validateRating(rating);

        // Check if user has already reviewed this game player
        List<Review> existingReviews = reviewRepository.findByGamePlayerIdAndUserId(gamePlayerId, userId);
        if (!existingReviews.isEmpty()) {
            throw new ReviewException("User has already reviewed this game player");
        }

        Review review = new Review();
        review.setGamePlayer(gamePlayer);
        review.setUser(user);
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(LocalDateTime.now());
        review.setStatus("ACTIVE");

        return reviewRepository.save(review);
    }

    public Review updateReview(Long reviewId, Double rating, String comment) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        validateRating(rating);

        review.setRating(rating);
        review.setComment(comment);
        review.setUpdatedAt(LocalDateTime.now());

        return reviewRepository.save(review);
    }

    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        review.setStatus("DELETED");
        reviewRepository.save(review);
    }

    public List<Review> getGamePlayerReviews(Long gamePlayerId) {
        return reviewRepository.findByGamePlayerIdAndStatus(gamePlayerId, "ACTIVE");
    }

    public List<Review> getUserReviews(Long userId) {
        return reviewRepository.findByUserId(userId);
    }

    public Double getGamePlayerAverageRating(Long gamePlayerId) {
        return reviewRepository.findAverageRatingByGamePlayerId(gamePlayerId);
    }

    public List<Review> getReviewsByStatus(String status) {
        validateStatus(status);
        return reviewRepository.findByStatus(status);
    }

    private void validateRating(Double rating) {
        if (rating < 1.0 || rating > 5.0) {
            throw new ReviewException("Rating must be between 1.0 and 5.0");
        }
    }

    private void validateStatus(String status) {
        if (!Arrays.asList("ACTIVE", "HIDDEN", "DELETED").contains(status)) {
            throw new IllegalArgumentException("Invalid review status");
        }
    }
} 