package com.example.backend.repository;

import com.example.backend.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByGamePlayerId(Long gamePlayerId);
    List<Schedule> findByUserId(Long userId);
    List<Schedule> findByGamePlayerIdAndStartTimeBetween(Long gamePlayerId, LocalDateTime start, LocalDateTime end);
    List<Schedule> findByUserIdAndStartTimeBetween(Long userId, LocalDateTime start, LocalDateTime end);
    List<Schedule> findByStatus(String status);
} 