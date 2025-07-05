package com.example.backend.controller;

import com.example.backend.entity.Promotion;
import com.example.backend.service.PromotionService;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.PromotionException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/promotions")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Promotion", description = "Promotion management APIs")
public class PromotionController {
    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @Operation(summary = "Create a new promotion")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Promotion> createPromotion(@Valid @RequestBody PromotionRequest request) {
        try {
            Promotion promotion = promotionService.createPromotion(
                    request.getCode(),
                    request.getName(),
                    request.getDescription(),
                    request.getDiscountAmount(),
                    request.getDiscountType(),
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getMaxUses(),
                    request.getMinimumPurchase(),
                    request.getGameIds()
            );
            return ResponseEntity.ok(promotion);
        } catch (PromotionException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Update a promotion")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Promotion> updatePromotion(
            @Parameter(description = "Promotion ID") @PathVariable Long id,
            @Valid @RequestBody PromotionRequest request) {
        try {
            Promotion promotion = promotionService.updatePromotion(
                    id,
                    request.getName(),
                    request.getDescription(),
                    request.getDiscountAmount(),
                    request.getDiscountType(),
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getMaxUses(),
                    request.getMinimumPurchase(),
                    request.getGameIds()
            );
            return ResponseEntity.ok(promotion);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (PromotionException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Deactivate a promotion")
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Promotion> deactivatePromotion(
            @Parameter(description = "Promotion ID") @PathVariable Long id) {
        try {
            Promotion promotion = promotionService.deactivatePromotion(id);
            return ResponseEntity.ok(promotion);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get active promotions")
    @GetMapping("/active")
    public ResponseEntity<List<Promotion>> getActivePromotions() {
        return ResponseEntity.ok(promotionService.getActivePromotions());
    }

    @Operation(summary = "Get promotions by discount type")
    @GetMapping("/discount-type/{type}")
    public ResponseEntity<List<Promotion>> getPromotionsByDiscountType(
            @Parameter(description = "Discount type") @PathVariable String type) {
        return ResponseEntity.ok(promotionService.getPromotionsByDiscountType(type));
    }

    @Operation(summary = "Get promotions by game")
    @GetMapping("/game/{gameName}")
    public ResponseEntity<List<Promotion>> getPromotionsByGame(
            @Parameter(description = "Game name") @PathVariable String gameName) {
        return ResponseEntity.ok(promotionService.getPromotionsByGame(gameName));
    }

    @Operation(summary = "Calculate discount")
    @PostMapping("/calculate-discount")
    public ResponseEntity<BigDecimal> calculateDiscount(@Valid @RequestBody CalculateDiscountRequest request) {
        try {
            BigDecimal discount = promotionService.calculateDiscount(
                    request.getCode(),
                    request.getOriginalAmount(),
                    request.getGameName()
            );
            return ResponseEntity.ok(discount);
        } catch (PromotionException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllPromotions() {
        return ResponseEntity.ok(promotionService.getAllPromotions());
    }
}

@Data
class PromotionRequest {
    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Discount amount is required")
    @Positive(message = "Discount amount must be positive")
    private BigDecimal discountAmount;

    @NotBlank(message = "Discount type is required")
    private String discountType;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    private Integer maxUses;

    private BigDecimal minimumPurchase;

    @NotEmpty(message = "At least one game must be selected")
    private Set<Long> gameIds;
}

@Data
class CalculateDiscountRequest {
    @NotBlank(message = "Code is required")
    private String code;

    @NotNull(message = "Original amount is required")
    @Positive(message = "Original amount must be positive")
    private BigDecimal originalAmount;

    @NotBlank(message = "Game name is required")
    private String gameName;
} 