package com.example.backend.repository;

import com.example.backend.entity.Moment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MomentRepository extends JpaRepository<Moment, Long> {
    
    // Lấy tất cả moment của một game player
    Page<Moment> findByGamePlayerIdAndStatusOrderByCreatedAtDesc(Long gamePlayerId, String status, Pageable pageable);
    
    // Lấy moment theo game player ID
    List<Moment> findByGamePlayerIdAndStatus(Long gamePlayerId, String status);
    
    // Lấy moment theo user ID (thông qua game player)
    @Query("SELECT m FROM Moment m WHERE m.gamePlayer.user.id = ?1 AND m.status = ?2 ORDER BY m.createdAt DESC")
    Page<Moment> findByUserIdAndStatus(Long userId, String status, Pageable pageable);
    
    // Đếm số moment của một game player
    Long countByGamePlayerIdAndStatus(Long gamePlayerId, String status);
    
    // Lấy moment theo ID và game player ID
    Moment findByIdAndGamePlayerId(Long id, Long gamePlayerId);
    
    // Lấy moment theo ID và user ID
    @Query("SELECT m FROM Moment m WHERE m.id = ?1 AND m.gamePlayer.user.id = ?2")
    Moment findByIdAndUserId(Long id, Long userId);
    
    // Lấy moment theo danh sách game player IDs (cho feed)
    @Query("SELECT m FROM Moment m WHERE m.gamePlayer.id IN ?1 AND m.status = ?2 ORDER BY m.createdAt DESC")
    Page<Moment> findByGamePlayerIdInAndStatusOrderByCreatedAtDesc(List<Long> gamePlayerIds, String status, Pageable pageable);

    Page<Moment> findByStatusOrderByCreatedAtDesc(String status, org.springframework.data.domain.Pageable pageable);
} 