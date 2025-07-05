package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "renter_id")
    private User renter;

    @ManyToOne
    @JoinColumn(name = "player_id")
    private GamePlayer player;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long price;
    private String status; // PENDING, CONFIRMED, CANCELED, COMPLETED, ...

    @OneToOne
    @JoinColumn(name = "payment_id")
    private Payment payment;
} 