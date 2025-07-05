package com.example.backend.dto;

import lombok.Data;

@Data
public class HireStatsDTO {
    private String period; // Thời gian (tháng/năm)
    private Integer totalHires; // Tổng số lượt thuê
    private Integer completedHires; // Số lượt hoàn thành
    private Integer totalHours; // Tổng số giờ
    private Long earnings; // Tổng thu nhập (xu)
} 