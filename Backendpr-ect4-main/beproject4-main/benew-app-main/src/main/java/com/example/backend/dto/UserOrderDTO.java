package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserOrderDTO {
    private Long id;
    private String renterName;
    private String playerName;
    private String hireTime;
    private String status;
    private Long price;
    private String orderType; // "HIRED" (đơn thuê) hoặc "HIRING" (đơn được thuê)
    private String game;
    private String playerRank;
    private String playerAvatarUrl;
    private String renterAvatarUrl;
    private Long hours;
    private String statusLabel; // Label tiếng Việt
} 