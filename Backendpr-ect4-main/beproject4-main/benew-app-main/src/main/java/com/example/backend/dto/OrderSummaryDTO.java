package com.example.backend.dto;

import lombok.Data;

@Data
public class OrderSummaryDTO {
    private Long id;
    private String renterName;
    private String playerName;
    private String timeRange;
    private Long price;
    private String statusLabel;
    private String renterAvatarUrl;
    private String playerAvatarUrl;
} 