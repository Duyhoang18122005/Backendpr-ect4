package com.example.backend.controller;

import com.example.backend.entity.Schedule;
import com.example.backend.service.ScheduleService;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.ScheduleConflictException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Schedule", description = "Schedule management APIs")
public class ScheduleController {
    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @Operation(summary = "Create a new schedule")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Schedule> createSchedule(
            @Valid @RequestBody ScheduleRequest request,
            Authentication authentication) {
        try {
            Schedule schedule = scheduleService.createSchedule(
                    request.getGamePlayerId(),
                    Long.parseLong(authentication.getName()),
                    request.getStartTime(),
                    request.getEndTime(),
                    request.getNotes()
            );
            return ResponseEntity.ok(schedule);
        } catch (ScheduleConflictException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Update a schedule")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Schedule> updateSchedule(
            @Parameter(description = "Schedule ID") @PathVariable Long id,
            @Valid @RequestBody ScheduleRequest request) {
        try {
            Schedule schedule = scheduleService.updateSchedule(
                    id,
                    request.getStartTime(),
                    request.getEndTime(),
                    request.getNotes()
            );
            return ResponseEntity.ok(schedule);
        } catch (ScheduleConflictException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Cancel a schedule")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelSchedule(
            @Parameter(description = "Schedule ID") @PathVariable Long id) {
        try {
            scheduleService.cancelSchedule(id);
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get game player schedules")
    @GetMapping("/game-player/{gamePlayerId}")
    public ResponseEntity<List<Schedule>> getGamePlayerSchedules(
            @Parameter(description = "Game player ID") @PathVariable Long gamePlayerId) {
        return ResponseEntity.ok(scheduleService.getGamePlayerSchedules(gamePlayerId));
    }

    @Operation(summary = "Get user schedules")
    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Schedule>> getUserSchedules(Authentication authentication) {
        return ResponseEntity.ok(scheduleService.getUserSchedules(
                Long.parseLong(authentication.getName())));
    }

    @Operation(summary = "Get schedules by status")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Schedule>> getSchedulesByStatus(
            @Parameter(description = "Schedule status") @PathVariable String status) {
        return ResponseEntity.ok(scheduleService.getSchedulesByStatus(status));
    }
}

@Data
class ScheduleRequest {
    @NotNull(message = "Game player ID is required")
    private Long gamePlayerId;

    @NotNull(message = "Start time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    private String notes;
} 