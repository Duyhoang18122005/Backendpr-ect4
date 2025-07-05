package com.example.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "player_id")
    private User player;

    @ManyToOne
    @JoinColumn(name = "game_player_id")
    private GamePlayer gamePlayer;

    @Column(nullable = false)
    private String currency;

    @NotNull
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @NotNull
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column
    private String transactionId;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    private String description;

    @ManyToOne
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @NotNull
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentType type;

    @Column
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Long coin;

    @Column(name = "vnp_txn_ref")
    private String vnpTxnRef;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum PaymentStatus {
        PENDING, CONFIRMED, COMPLETED, FAILED, REFUNDED, CANCELED
    }

    public enum PaymentMethod {
        TOPUP, HIRE, WITHDRAW, DONATE, BANK_TRANSFER, MOMO, VNPAY, ZALOPAY
    }

    public enum PaymentType {
        TOPUP, HIRE, WITHDRAW, DONATE
    }
} 