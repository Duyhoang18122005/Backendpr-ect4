package com.example.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PlayerStatsDTO {
    private Long playerId;
    private String playerName;
    private Integer totalHires;
    private Integer completedHires;
    private Integer totalHireHours;
    private Double completionRate;
    private Long totalEarnings;
    private Double averageRating;
    private Integer totalReviews;
    private List<ReviewDTO> recentReviews;
    private List<HireStatsDTO> hireStats;
}
