package com.example.backend.controller;

import com.example.backend.repository.RevenueRepository;
import com.example.backend.repository.PaymentRepository;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/statistics")
@CrossOrigin(origins = "http://localhost:3000")
public class RevenueController {
    private final RevenueRepository revenueRepository;
    private final PaymentRepository paymentRepository;

    public RevenueController(RevenueRepository revenueRepository, PaymentRepository paymentRepository) {
        this.revenueRepository = revenueRepository;
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/revenue-orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RevenueOrderStat>> getRevenueAndOrders(
            @RequestParam("from") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam("to") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);
        // Lấy revenue từng ngày
        List<Object[]> revenueList = revenueRepository.findDailyRevenueBetween(start, end);
        // Lấy order count từng ngày
        List<Object[]> orderList = paymentRepository.findDailyOrderCountBetween(start, end);
        // Map ngày -> revenue
        Map<String, Long> revenueMap = new HashMap<>();
        for (Object[] row : revenueList) {
            String date = row[0].toString();
            Long total = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            revenueMap.put(date, total);
        }
        // Map ngày -> orders
        Map<String, Long> orderMap = new HashMap<>();
        for (Object[] row : orderList) {
            String date = row[0].toString();
            Long total = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            orderMap.put(date, total);
        }
        // Build kết quả cho từng ngày
        List<RevenueOrderStat> result = new ArrayList<>();
        LocalDate d = from;
        while (!d.isAfter(to)) {
            String dateStr = d.toString();
            long revenue = revenueMap.getOrDefault(dateStr, 0L);
            long orders = orderMap.getOrDefault(dateStr, 0L);
            result.add(new RevenueOrderStat(dateStr, revenue, orders));
            d = d.plusDays(1);
        }
        return ResponseEntity.ok(result);
    }
}

@Data
class RevenueOrderStat {
    private final String date;
    private final long revenue;
    private final long orders;
} 