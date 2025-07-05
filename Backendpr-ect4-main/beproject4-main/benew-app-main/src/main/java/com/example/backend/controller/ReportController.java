package com.example.backend.controller;

import com.example.backend.entity.Report;
import com.example.backend.service.ReportService;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.ReportException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Report", description = "Report management APIs")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(summary = "Create a new report")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Report> createReport(
            @Valid @RequestBody ReportRequest request,
            Authentication authentication) {
        try {
            Report report = reportService.createReport(
                    request.getReportedPlayerId(),
                    Long.parseLong(authentication.getName()),
                    request.getReason(),
                    request.getDescription()
            );
            return ResponseEntity.ok(report);
        } catch (ReportException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Update report status")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Report> updateReportStatus(
            @Parameter(description = "Report ID") @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        try {
            Report report = reportService.updateReportStatus(
                    id,
                    request.getStatus(),
                    request.getResolution()
            );
            return ResponseEntity.ok(report);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get reports by reporter")
    @GetMapping("/reporter")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Report>> getReportsByReporter(Authentication authentication) {
        return ResponseEntity.ok(reportService.getReportsByReporter(
                Long.parseLong(authentication.getName())));
    }

    @Operation(summary = "Get reports by reported player")
    @GetMapping("/reported-player/{reportedPlayerId}")
    public ResponseEntity<List<Report>> getReportsByReportedPlayer(
            @Parameter(description = "Reported player ID") @PathVariable Long reportedPlayerId) {
        return ResponseEntity.ok(reportService.getReportsByReportedPlayer(reportedPlayerId));
    }

    @Operation(summary = "Get reports by status")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Report>> getReportsByStatus(
            @Parameter(description = "Report status") @PathVariable String status) {
        return ResponseEntity.ok(reportService.getReportsByStatus(status));
    }

    @Operation(summary = "Get active reports")
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Report>> getActiveReports() {
        return ResponseEntity.ok(reportService.getActiveReports());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteReport(@PathVariable Long id) {
        reportService.deleteReport(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getReportSummary() {
        long total = reportService.getTotalReportCount();
        long unprocessed = reportService.getUnprocessedReportCount();
        return ResponseEntity.ok(new ReportSummaryResponse(total, unprocessed));
    }
}

@Data
class ReportRequest {
    @NotNull(message = "Reported player ID is required")
    private Long reportedPlayerId;

    @NotBlank(message = "Reason is required")
    private String reason;

    @NotBlank(message = "Description is required")
    private String description;
}

@Data
class UpdateStatusRequest {
    @NotBlank(message = "Status is required")
    private String status;

    private String resolution;
}

@Data
class ReportSummaryResponse {
    private final long total;
    private final long unprocessed;
} 