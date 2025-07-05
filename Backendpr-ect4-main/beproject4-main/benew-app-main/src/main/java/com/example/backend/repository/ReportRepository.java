package com.example.backend.repository;

import com.example.backend.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByReporterId(Long reporterId);
    List<Report> findByReportedPlayerId(Long reportedPlayerId);
    List<Report> findByStatus(String status);
    List<Report> findByReporterIdAndStatus(Long reporterId, String status);
    List<Report> findByReportedPlayerIdAndStatus(Long reportedPlayerId, String status);
    List<Report> findByReporterIdAndReportedPlayerId(Long reporterId, Long reportedPlayerId);
    long countByStatus(String status);
} 