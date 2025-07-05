package com.example.backend.controller;

import com.example.backend.entity.SupportTicket;
import com.example.backend.service.SupportTicketService;
import com.example.backend.exception.ResourceNotFoundException;
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
@RequestMapping("/api/support-tickets")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Support Ticket", description = "Support ticket management APIs")
public class SupportTicketController {
    private final SupportTicketService supportTicketService;

    public SupportTicketController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @Operation(summary = "Create a new support ticket")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SupportTicket> createTicket(
            @Valid @RequestBody TicketRequest request,
            Authentication authentication) {
        try {
            SupportTicket ticket = supportTicketService.createTicket(
                    Long.parseLong(authentication.getName()),
                    request.getTitle(),
                    request.getDescription(),
                    request.getCategory(),
                    request.getPriority()
            );
            return ResponseEntity.ok(ticket);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Update a support ticket")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SupportTicket> updateTicket(
            @Parameter(description = "Ticket ID") @PathVariable Long id,
            @Valid @RequestBody TicketRequest request) {
        try {
            SupportTicket ticket = supportTicketService.updateTicket(
                    id,
                    request.getTitle(),
                    request.getDescription(),
                    request.getCategory(),
                    request.getPriority()
            );
            return ResponseEntity.ok(ticket);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Assign a support ticket")
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SupportTicket> assignTicket(
            @Parameter(description = "Ticket ID") @PathVariable Long id,
            @Valid @RequestBody AssignRequest request) {
        try {
            SupportTicket ticket = supportTicketService.assignTicket(id, request.getAssignedToId());
            return ResponseEntity.ok(ticket);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Resolve a support ticket")
    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SupportTicket> resolveTicket(
            @Parameter(description = "Ticket ID") @PathVariable Long id,
            @Valid @RequestBody ResolveRequest request) {
        try {
            SupportTicket ticket = supportTicketService.resolveTicket(id, request.getResolution());
            return ResponseEntity.ok(ticket);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Close a support ticket")
    @PutMapping("/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SupportTicket> closeTicket(
            @Parameter(description = "Ticket ID") @PathVariable Long id) {
        try {
            SupportTicket ticket = supportTicketService.closeTicket(id);
            return ResponseEntity.ok(ticket);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get user tickets")
    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SupportTicket>> getUserTickets(Authentication authentication) {
        return ResponseEntity.ok(supportTicketService.getUserTickets(
                Long.parseLong(authentication.getName())));
    }

    @Operation(summary = "Get tickets by status")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<SupportTicket>> getTicketsByStatus(
            @Parameter(description = "Ticket status") @PathVariable String status) {
        return ResponseEntity.ok(supportTicketService.getTicketsByStatus(status));
    }

    @Operation(summary = "Get tickets by priority")
    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<SupportTicket>> getTicketsByPriority(
            @Parameter(description = "Ticket priority") @PathVariable String priority) {
        return ResponseEntity.ok(supportTicketService.getTicketsByPriority(priority));
    }

    @Operation(summary = "Get tickets by category")
    @GetMapping("/category/{category}")
    public ResponseEntity<List<SupportTicket>> getTicketsByCategory(
            @Parameter(description = "Ticket category") @PathVariable String category) {
        return ResponseEntity.ok(supportTicketService.getTicketsByCategory(category));
    }

    @Operation(summary = "Get assigned tickets")
    @GetMapping("/assigned")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SupportTicket>> getAssignedTickets(Authentication authentication) {
        return ResponseEntity.ok(supportTicketService.getAssignedTickets(
                Long.parseLong(authentication.getName())));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllTickets() {
        return ResponseEntity.ok(supportTicketService.getAllTickets());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTicket(@PathVariable Long id) {
        supportTicketService.deleteTicket(id);
        return ResponseEntity.ok().build();
    }
}

@Data
class TicketRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Priority is required")
    private String priority;
}

@Data
class AssignRequest {
    @NotNull(message = "Assigned to ID is required")
    private Long assignedToId;
}

@Data
class ResolveRequest {
    @NotBlank(message = "Resolution is required")
    private String resolution;
} 