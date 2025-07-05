package com.example.backend.repository;

import com.example.backend.entity.Revenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RevenueRepository extends JpaRepository<Revenue, Long> {
    @Query("SELECT SUM(r.amount) FROM Revenue r WHERE r.createdAt BETWEEN :start AND :end")
    Long sumRevenueByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT DATE(created_at) as date, SUM(amount) as total FROM revenues WHERE created_at BETWEEN :start AND :end GROUP BY DATE(created_at) ORDER BY DATE(created_at)", nativeQuery = true)
    List<Object[]> findDailyRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
} 