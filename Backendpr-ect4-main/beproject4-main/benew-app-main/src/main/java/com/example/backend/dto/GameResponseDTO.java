package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GameResponseDTO {
    private Long id;
    private String name;
    private String imageUrl;
    private String category;
    private String status;
    private long playerCount;
    private List<String> availableRoles;
    private List<String> availableRanks;
} 