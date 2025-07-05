package com.example.backend.service;

import com.example.backend.entity.Promotion;
import com.example.backend.entity.Game;
import com.example.backend.repository.PromotionRepository;
import com.example.backend.repository.GameRepository;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.PromotionException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class PromotionService {
    private final PromotionRepository promotionRepository;
    private final GameRepository gameRepository;

    public PromotionService(PromotionRepository promotionRepository,
                          GameRepository gameRepository) {
        this.promotionRepository = promotionRepository;
        this.gameRepository = gameRepository;
    }

    public Promotion createPromotion(String code, String name, String description,
                                   BigDecimal discountAmount, String discountType,
                                   LocalDateTime startDate, LocalDateTime endDate,
                                   Integer maxUses, BigDecimal minimumPurchase,
                                   Set<Long> gameIds) {
        validateDiscountType(discountType);
        validateDates(startDate, endDate);

        // Check if promotion code already exists
        if (promotionRepository.findByCode(code).isPresent()) {
            throw new PromotionException("Promotion code already exists");
        }

        // Get games by IDs
        Set<Game> games = new HashSet<>();
        for (Long gameId : gameIds) {
            Game game = gameRepository.findById(gameId)
                    .orElseThrow(() -> new ResourceNotFoundException("Game not found with id: " + gameId));
            games.add(game);
        }

        Promotion promotion = new Promotion();
        promotion.setCode(code);
        promotion.setName(name);
        promotion.setDescription(description);
        promotion.setDiscountAmount(discountAmount);
        promotion.setDiscountType(discountType);
        promotion.setStartDate(startDate);
        promotion.setEndDate(endDate);
        promotion.setActive(true);
        promotion.setMaxUses(maxUses);
        promotion.setCurrentUses(0);
        promotion.setMinimumPurchase(minimumPurchase);
        promotion.setApplicableGames(games);

        return promotionRepository.save(promotion);
    }

    public Promotion updatePromotion(Long promotionId, String name, String description,
                                   BigDecimal discountAmount, String discountType,
                                   LocalDateTime startDate, LocalDateTime endDate,
                                   Integer maxUses, BigDecimal minimumPurchase,
                                   Set<Long> gameIds) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));

        validateDiscountType(discountType);
        validateDates(startDate, endDate);

        // Get games by IDs
        Set<Game> games = new HashSet<>();
        for (Long gameId : gameIds) {
            Game game = gameRepository.findById(gameId)
                    .orElseThrow(() -> new ResourceNotFoundException("Game not found with id: " + gameId));
            games.add(game);
        }

        promotion.setName(name);
        promotion.setDescription(description);
        promotion.setDiscountAmount(discountAmount);
        promotion.setDiscountType(discountType);
        promotion.setStartDate(startDate);
        promotion.setEndDate(endDate);
        promotion.setMaxUses(maxUses);
        promotion.setMinimumPurchase(minimumPurchase);
        promotion.setApplicableGames(games);

        return promotionRepository.save(promotion);
    }

    public Promotion deactivatePromotion(Long promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));

        promotion.setActive(false);
        return promotionRepository.save(promotion);
    }

    public List<Promotion> getActivePromotions() {
        LocalDateTime now = LocalDateTime.now();
        return promotionRepository.findByStartDateBeforeAndEndDateAfter(now, now);
    }

    public List<Promotion> getPromotionsByDiscountType(String discountType) {
        validateDiscountType(discountType);
        return promotionRepository.findByDiscountType(discountType);
    }

    public List<Promotion> getPromotionsByGame(String gameName) {
        return promotionRepository.findByApplicableGamesName(gameName);
    }

    public BigDecimal calculateDiscount(String code, BigDecimal originalAmount, String gameName) {
        Promotion promotion = promotionRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));

        if (!promotion.getActive()) {
            throw new PromotionException("Promotion is not active");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(promotion.getStartDate()) || now.isAfter(promotion.getEndDate())) {
            throw new PromotionException("Promotion is not valid at this time");
        }

        if (promotion.getMaxUses() != null && 
            promotion.getCurrentUses() >= promotion.getMaxUses()) {
            throw new PromotionException("Promotion has reached maximum uses");
        }

        if (promotion.getMinimumPurchase() != null && 
            originalAmount.compareTo(promotion.getMinimumPurchase()) < 0) {
            throw new PromotionException("Minimum purchase amount not met");
        }

        boolean isGameApplicable = promotion.getApplicableGames().stream()
                .anyMatch(game -> game.getName().equals(gameName));
        if (!isGameApplicable) {
            throw new PromotionException("Promotion is not applicable for this game");
        }

        BigDecimal discount;
        if ("PERCENTAGE".equals(promotion.getDiscountType())) {
            discount = originalAmount.multiply(promotion.getDiscountAmount())
                    .divide(new BigDecimal("100"));
        } else {
            discount = promotion.getDiscountAmount();
        }

        // Update current uses
        promotion.setCurrentUses(promotion.getCurrentUses() + 1);
        promotionRepository.save(promotion);

        return discount;
    }

    public List<Promotion> getAllPromotions() {
        return promotionRepository.findAll();
    }

    private void validateDiscountType(String discountType) {
        if (!Arrays.asList("PERCENTAGE", "FIXED_AMOUNT").contains(discountType)) {
            throw new IllegalArgumentException("Invalid discount type");
        }
    }

    private void validateDates(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
    }
}