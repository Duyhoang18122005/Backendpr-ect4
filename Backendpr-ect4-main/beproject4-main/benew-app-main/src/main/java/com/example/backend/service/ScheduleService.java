package com.example.backend.service;

import com.example.backend.entity.Schedule;
import com.example.backend.entity.GamePlayer;
import com.example.backend.entity.User;
import com.example.backend.repository.ScheduleRepository;
import com.example.backend.repository.GamePlayerRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.ScheduleConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final UserRepository userRepository;

    public ScheduleService(ScheduleRepository scheduleRepository,
                          GamePlayerRepository gamePlayerRepository,
                          UserRepository userRepository) {
        this.scheduleRepository = scheduleRepository;
        this.gamePlayerRepository = gamePlayerRepository;
        this.userRepository = userRepository;
    }

    public Schedule createSchedule(Long gamePlayerId, Long userId, LocalDateTime startTime,
                                 LocalDateTime endTime, String notes) {
        GamePlayer gamePlayer = gamePlayerRepository.findById(gamePlayerId)
                .orElseThrow(() -> new ResourceNotFoundException("Game player not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check for schedule conflicts
        List<Schedule> existingSchedules = scheduleRepository
                .findByGamePlayerIdAndStartTimeBetween(gamePlayerId, startTime, endTime);
        if (!existingSchedules.isEmpty()) {
            throw new ScheduleConflictException("Time slot is already booked");
        }

        Schedule schedule = new Schedule();
        schedule.setGamePlayer(gamePlayer);
        schedule.setUser(user);
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);
        schedule.setNotes(notes);
        schedule.setStatus("PENDING");
        schedule.setTotalPrice(calculateTotalPrice(gamePlayer, startTime, endTime));

        return scheduleRepository.save(schedule);
    }

    public Schedule updateSchedule(Long scheduleId, LocalDateTime startTime,
                                 LocalDateTime endTime, String notes) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

        // Check for schedule conflicts excluding current schedule
        List<Schedule> existingSchedules = scheduleRepository
                .findByGamePlayerIdAndStartTimeBetween(schedule.getGamePlayer().getId(), startTime, endTime);
        existingSchedules.removeIf(s -> s.getId().equals(scheduleId));
        if (!existingSchedules.isEmpty()) {
            throw new ScheduleConflictException("Time slot is already booked");
        }

        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);
        schedule.setNotes(notes);
        schedule.setTotalPrice(calculateTotalPrice(schedule.getGamePlayer(), startTime, endTime));

        return scheduleRepository.save(schedule);
    }

    public void cancelSchedule(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        schedule.setStatus("CANCELLED");
        scheduleRepository.save(schedule);
    }

    public List<Schedule> getGamePlayerSchedules(Long gamePlayerId) {
        return scheduleRepository.findByGamePlayerId(gamePlayerId);
    }

    public List<Schedule> getUserSchedules(Long userId) {
        return scheduleRepository.findByUserId(userId);
    }

    public List<Schedule> getSchedulesByStatus(String status) {
        return scheduleRepository.findByStatus(status);
    }

    private Double calculateTotalPrice(GamePlayer gamePlayer, LocalDateTime startTime, LocalDateTime endTime) {
        long hours = java.time.Duration.between(startTime, endTime).toHours();
        return gamePlayer.getPricePerHour().doubleValue() * hours;
    }
} 