package com.example.backend.service;

import com.example.backend.entity.SupportTicket;
import com.example.backend.entity.User;
import com.example.backend.repository.SupportTicketRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class SupportTicketService {
    private final SupportTicketRepository supportTicketRepository;
    private final UserRepository userRepository;

    public SupportTicketService(SupportTicketRepository supportTicketRepository,
                              UserRepository userRepository) {
        this.supportTicketRepository = supportTicketRepository;
        this.userRepository = userRepository;
    }

    public SupportTicket createTicket(Long userId, String title, String description,
                                    String category, String priority) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        validateCategory(category);
        validatePriority(priority);

        SupportTicket ticket = new SupportTicket();
        ticket.setUser(user);
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setCategory(category);
        ticket.setPriority(priority);
        ticket.setStatus("OPEN");
        ticket.setCreatedAt(LocalDateTime.now());

        return supportTicketRepository.save(ticket);
    }

    public SupportTicket updateTicket(Long ticketId, String title, String description,
                                    String category, String priority) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        validateCategory(category);
        validatePriority(priority);

        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setCategory(category);
        ticket.setPriority(priority);
        ticket.setUpdatedAt(LocalDateTime.now());

        return supportTicketRepository.save(ticket);
    }

    public SupportTicket assignTicket(Long ticketId, Long assignedToId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        User assignedTo = userRepository.findById(assignedToId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ticket.setAssignedTo(assignedTo);
        ticket.setStatus("IN_PROGRESS");
        ticket.setUpdatedAt(LocalDateTime.now());

        return supportTicketRepository.save(ticket);
    }

    public SupportTicket resolveTicket(Long ticketId, String resolution) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        ticket.setStatus("RESOLVED");
        ticket.setResolution(resolution);
        ticket.setResolvedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        return supportTicketRepository.save(ticket);
    }

    public SupportTicket closeTicket(Long ticketId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        ticket.setStatus("CLOSED");
        ticket.setUpdatedAt(LocalDateTime.now());

        return supportTicketRepository.save(ticket);
    }

    public List<SupportTicket> getUserTickets(Long userId) {
        return supportTicketRepository.findByUserId(userId);
    }

    public List<SupportTicket> getTicketsByStatus(String status) {
        validateStatus(status);
        return supportTicketRepository.findByStatus(status);
    }

    public List<SupportTicket> getTicketsByPriority(String priority) {
        validatePriority(priority);
        return supportTicketRepository.findByPriority(priority);
    }

    public List<SupportTicket> getTicketsByCategory(String category) {
        validateCategory(category);
        return supportTicketRepository.findByCategory(category);
    }

    public List<SupportTicket> getAssignedTickets(Long assignedToId) {
        return supportTicketRepository.findByAssignedToId(assignedToId);
    }

    public List<SupportTicket> getAllTickets() {
        return supportTicketRepository.findAll();
    }

    public void deleteTicket(Long id) {
        SupportTicket ticket = supportTicketRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        supportTicketRepository.delete(ticket);
    }

    private void validatePriority(String priority) {
        if (!Arrays.asList("LOW", "MEDIUM", "HIGH", "URGENT").contains(priority)) {
            throw new IllegalArgumentException("Invalid priority level");
        }
    }

    private void validateCategory(String category) {
        if (!Arrays.asList("TECHNICAL", "PAYMENT", "SCHEDULE", "REPORT", "OTHER").contains(category)) {
            throw new IllegalArgumentException("Invalid ticket category");
        }
    }

    private void validateStatus(String status) {
        if (!Arrays.asList("OPEN", "IN_PROGRESS", "WAITING_FOR_USER", "RESOLVED", "CLOSED").contains(status)) {
            throw new IllegalArgumentException("Invalid ticket status");
        }
    }
} 