package com.example.backend.controller;

import com.example.backend.entity.Review;
import com.example.backend.service.ReviewService;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.ReviewException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Review", description = "Review management APIs")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "Create a new review")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Review> createReview(
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        try {
            Review review = reviewService.createReview(
                    request.getGamePlayerId(),
                    Long.parseLong(authentication.getName()),
                    request.getRating(),
                    request.getComment()
            );
            return ResponseEntity.ok(review);
        } catch (ReviewException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Update a review")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Review> updateReview(
            @Parameter(description = "Review ID") @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request) {
        try {
            Review review = reviewService.updateReview(
                    id,
                    request.getRating(),
                    request.getComment()
            );
            return ResponseEntity.ok(review);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Delete a review")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteReview(
            @Parameter(description = "Review ID") @PathVariable Long id) {
        try {
            reviewService.deleteReview(id);
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get game player reviews")
    @GetMapping("/game-player/{gamePlayerId}")
    public ResponseEntity<List<Review>> getGamePlayerReviews(
            @Parameter(description = "Game player ID") @PathVariable Long gamePlayerId) {
        return ResponseEntity.ok(reviewService.getGamePlayerReviews(gamePlayerId));
    }

    @Operation(summary = "Get user reviews")
    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Review>> getUserReviews(Authentication authentication) {
        return ResponseEntity.ok(reviewService.getUserReviews(
                Long.parseLong(authentication.getName())));
    }

    @Operation(summary = "Get game player average rating")
    @GetMapping("/game-player/{gamePlayerId}/average-rating")
    public ResponseEntity<Double> getGamePlayerAverageRating(
            @Parameter(description = "Game player ID") @PathVariable Long gamePlayerId) {
        return ResponseEntity.ok(reviewService.getGamePlayerAverageRating(gamePlayerId));
    }

    @Operation(summary = "Get reviews by status")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Review>> getReviewsByStatus(
            @Parameter(description = "Review status") @PathVariable String status) {
        return ResponseEntity.ok(reviewService.getReviewsByStatus(status));
    }
}

@Data
class ReviewRequest {
    @NotNull(message = "Game player ID is required")
    private Long gamePlayerId;

    @NotNull(message = "Rating is required")
    @DecimalMin(value = "1.0", message = "Rating must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Rating must be at most 5.0")
    private Double rating;

    @NotBlank(message = "Comment is required")
    private String comment;
} 