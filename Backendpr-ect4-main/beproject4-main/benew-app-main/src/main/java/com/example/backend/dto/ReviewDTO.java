package com.example.backend.dto;

import lombok.Data;

@Data
public class ReviewDTO {
    private Long reviewId;
    private Integer rating;
    private String comment;
    private String reviewerName;
    private String createdAt;
} 