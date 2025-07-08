package com.example.backend.service;

import com.example.backend.entity.*;
import com.example.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PlayerRewardService {
    private static final List<Long> MILESTONES = Arrays.asList(1000L, 1500L, 2500L, 5000L, 10000L); // phút
    private static final List<Long> REWARDS = Arrays.asList(20000L, 30000L, 50000L, 100000L, 1000000L); // xu

    @Autowired
    private PlayerRewardHistoryRepository rewardHistoryRepo;
    @Autowired
    private GamePlayerRepository gamePlayerRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RevenueRepository revenueRepository;

    @Transactional
    public void processOrderCompleted(Order order) {
        if (order.getPlayer() == null || order.getStartTime() == null || order.getEndTime() == null) return;
        GamePlayer player = order.getPlayer();
        long minutes = Duration.between(order.getStartTime(), order.getEndTime()).toMinutes();
        if (minutes <= 0) return;
        Long totalMinutes = player.getTotalMinutesHired() == null ? 0L : player.getTotalMinutesHired();
        totalMinutes += minutes;
        player.setTotalMinutesHired(totalMinutes);
        Integer lastMilestone = player.getLastRewardMilestone() == null ? 0 : player.getLastRewardMilestone();
        for (int i = lastMilestone; i < MILESTONES.size(); i++) {
            if (totalMinutes >= MILESTONES.get(i)) {
                long rewardCoin = REWARDS.get(i);
                User user = player.getUser();
                if (user != null) {
                    user.setCoin(user.getCoin() + rewardCoin);
                    userRepository.save(user);
                }
                // Trừ vào doanh thu app (giả sử có 1 bản ghi doanh thu tổng)
                List<Revenue> revenues = revenueRepository.findAll();
                if (!revenues.isEmpty()) {
                    Revenue appRevenue = revenues.get(0);
                    appRevenue.setAmount(appRevenue.getAmount() - rewardCoin);
                    revenueRepository.save(appRevenue);
                }
                PlayerRewardHistory history = new PlayerRewardHistory();
                history.setPlayer(player);
                history.setMilestone(i + 1);
                history.setMinutesRequired(MILESTONES.get(i));
                history.setRewardCoin(rewardCoin);
                history.setRewardedAt(LocalDateTime.now());
                rewardHistoryRepo.save(history);
                player.setLastRewardMilestone(i + 1);
            }
        }
        gamePlayerRepository.save(player);
    }

    public List<PlayerRewardHistory> getRewardHistory(Long playerId) {
        return rewardHistoryRepo.findByPlayerId(playerId);
    }

    public Map<String, Object> getRewardStatus(Long playerId) {
        GamePlayer player = gamePlayerRepository.findById(playerId).orElse(null);
        if (player == null) return null;
        Long totalMinutes = player.getTotalMinutesHired() == null ? 0L : player.getTotalMinutesHired();
        Integer lastMilestone = player.getLastRewardMilestone() == null ? 0 : player.getLastRewardMilestone();
        int nextMilestoneIdx = lastMilestone < MILESTONES.size() ? lastMilestone : MILESTONES.size() - 1;
        Map<String, Object> result = new HashMap<>();
        result.put("totalMinutes", totalMinutes);
        result.put("lastMilestone", lastMilestone);
        result.put("nextMilestone", nextMilestoneIdx < MILESTONES.size() ? MILESTONES.get(nextMilestoneIdx) : null);
        result.put("nextReward", nextMilestoneIdx < REWARDS.size() ? REWARDS.get(nextMilestoneIdx) : null);
        return result;
    }
} 