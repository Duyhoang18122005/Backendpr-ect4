package com.example.backend.controller;

import com.example.backend.entity.PlayerRewardHistory;
import com.example.backend.service.PlayerRewardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/player-rewards")
public class PlayerRewardController {
    @Autowired
    private PlayerRewardService playerRewardService;

    // Lấy trạng thái phần thưởng hiện tại của player
    @GetMapping("/status/{playerId}")
    public Map<String, Object> getRewardStatus(@PathVariable Long playerId) {
        return playerRewardService.getRewardStatus(playerId);
    }

    // Lấy lịch sử nhận thưởng của player
    @GetMapping("/history/{playerId}")
    public List<PlayerRewardHistory> getRewardHistory(@PathVariable Long playerId) {
        return playerRewardService.getRewardHistory(playerId);
    }
} 