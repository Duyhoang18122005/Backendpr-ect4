package com.example.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "game_players")
public class GamePlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username is required")
    @Column(nullable = false)
    private String username;

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @NotBlank(message = "Rank is required")
    @Column(nullable = false)
    private String rank; // Rank trong game (ví dụ: Gold, Diamond, etc.)

    @Column
    private String role; // Vai trò trong game (ví dụ: Mid, Support, etc.) - Có thể null nếu game không cần role

    @NotBlank(message = "Server is required")
    @Column(nullable = false)
    private String server; // Server chơi (ví dụ: NA, EU, etc.)

    @NotNull(message = "Price per hour is required")
    @DecimalMin(value = "0.0", message = "Price must be greater than 0")
    @Column(nullable = false)
    private BigDecimal pricePerHour; // Giá thuê theo giờ

    @NotBlank(message = "Status is required")
    @Column(nullable = false)
    private String status = "AVAILABLE"; // Default value

    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description; // Mô tả về người chơi

    @Min(value = 0, message = "Rating must be greater than or equal to 0")
    @Max(value = 5, message = "Rating must be less than or equal to 5")
    private Double rating;

    @Min(value = 0, message = "Total games must be greater than or equal to 0")
    private Integer totalGames = 0; // Số game đã chơi

    @Min(value = 0, message = "Win rate must be greater than or equal to 0")
    @Max(value = 100, message = "Win rate must be less than or equal to 100")
    private Integer winRate = 0; // Tỷ lệ thắng (%)

    @ManyToOne
    @JoinColumn(name = "hired_by")
    private User hiredBy;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime hireDate;
    private LocalDateTime returnDate;

    @Min(value = 1, message = "Hours hired must be at least 1")
    private Integer hoursHired; // Số giờ thuê

    @Column(name = "total_minutes_hired")
    private Long totalMinutesHired = 0L;

    @Column(name = "last_reward_milestone")
    private Integer lastRewardMilestone = 0; // 0: chưa nhận, 1: mốc 1, ...

    // Thông tin ban
    @Column(name = "ban_reason")
    private String banReason; // Lý do ban

    @Column(name = "ban_description")
    private String banDescription; // Mô tả chi tiết lý do ban

    @Column(name = "banned_at")
    private LocalDateTime bannedAt; // Thời gian bị ban

    @ManyToOne
    @JoinColumn(name = "banned_by")
    private User bannedBy; // Admin đã ban

    @PrePersist
    @PreUpdate
    public void validateData() {
        if (rating != null && (rating < 0 || rating > 5)) {
            throw new IllegalArgumentException("Rating must be between 0 and 5");
        }
        if (winRate != null && (winRate < 0 || winRate > 100)) {
            throw new IllegalArgumentException("Win rate must be between 0 and 100");
        }
        // Kiểm tra role nếu game yêu cầu role
        if (game != null && game.getHasRoles() && (role == null || role.trim().isEmpty())) {
            throw new IllegalArgumentException("Role is required for this game");
        }
    }
} 