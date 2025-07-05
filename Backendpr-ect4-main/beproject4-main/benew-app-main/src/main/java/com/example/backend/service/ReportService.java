package com.example.backend.service;

import com.example.backend.entity.Report;
import com.example.backend.entity.GamePlayer;
import com.example.backend.entity.User;
import com.example.backend.repository.ReportRepository;
import com.example.backend.repository.GamePlayerRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.ReportException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class ReportService {
    private final ReportRepository reportRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final UserRepository userRepository;

    public ReportService(ReportRepository reportRepository,
                        GamePlayerRepository gamePlayerRepository,
                        UserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.gamePlayerRepository = gamePlayerRepository;
        this.userRepository = userRepository;
    }

    public Report createReport(Long reportedPlayerId, Long reporterId, String reason, String description) {
        GamePlayer reportedPlayer = gamePlayerRepository.findById(reportedPlayerId)
                .orElseThrow(() -> new ResourceNotFoundException("Reported player not found"));
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Reporter not found"));

        // Check if reporter has already reported this player
        List<Report> existingReports = reportRepository.findByReporterIdAndReportedPlayerId(reporterId, reportedPlayerId);
        if (!existingReports.isEmpty()) {
            throw new ReportException("User has already reported this player");
        }

        Report report = new Report();
        report.setReportedPlayer(reportedPlayer);
        report.setReporter(reporter);
        report.setReason(reason);
        report.setDescription(description);
        report.setStatus("PENDING");
        report.setCreatedAt(LocalDateTime.now());

        return reportRepository.save(report);
    }

    public Report updateReportStatus(Long reportId, String status, String resolution) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        validateStatus(status);

        report.setStatus(status);
        if ("RESOLVED".equals(status)) {
            report.setResolvedAt(LocalDateTime.now());
            report.setResolution(resolution);
        }

        return reportRepository.save(report);
    }

    public List<Report> getReportsByReporter(Long reporterId) {
        return reportRepository.findByReporterId(reporterId);
    }

    public List<Report> getReportsByReportedPlayer(Long reportedPlayerId) {
        return reportRepository.findByReportedPlayerId(reportedPlayerId);
    }

    public List<Report> getReportsByStatus(String status) {
        validateStatus(status);
        return reportRepository.findByStatus(status);
    }

    public List<Report> getActiveReports() {
        return reportRepository.findByStatus("PENDING");
    }

    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    public void deleteReport(Long id) {
        Report report = reportRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        reportRepository.delete(report);
    }

    public long getTotalReportCount() {
        return reportRepository.count();
    }

    public long getUnprocessedReportCount() {
        return reportRepository.countByStatus("PENDING");
    }

    private void validateStatus(String status) {
        if (!Arrays.asList("PENDING", "INVESTIGATING", "RESOLVED", "REJECTED").contains(status)) {
            throw new IllegalArgumentException("Invalid report status");
        }
    }
} 