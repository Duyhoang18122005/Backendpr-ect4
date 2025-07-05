package com.example.backend.repository;

import com.example.backend.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
    
    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
    
    List<UserBlock> findByBlockerId(Long blockerId);
    
    List<UserBlock> findByBlockedId(Long blockedId);
    
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM UserBlock b " +
           "WHERE (b.blocker.id = ?1 AND b.blocked.id = ?2) OR (b.blocker.id = ?2 AND b.blocked.id = ?1)")
    boolean isBlocked(Long userId1, Long userId2);
} 