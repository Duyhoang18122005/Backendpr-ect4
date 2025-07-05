package com.example.backend.repository;

import com.example.backend.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByUserId(Long userId);
    List<SupportTicket> findByStatus(String status);
    List<SupportTicket> findByPriority(String priority);
    List<SupportTicket> findByCategory(String category);
    List<SupportTicket> findByAssignedToId(Long assignedToId);
    List<SupportTicket> findByUserIdAndStatus(Long userId, String status);
    List<SupportTicket> findByAssignedToIdAndStatus(Long assignedToId, String status);
} 