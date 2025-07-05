package com.example.backend.repository;

import com.example.backend.entity.PlayerFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PlayerFollowRepository extends JpaRepository<PlayerFollow, Long> {
    boolean existsByFollowerIdAndGamePlayerId(Long followerId, Long gamePlayerId);
    
    void deleteByFollowerIdAndGamePlayerId(Long followerId, Long gamePlayerId);
    
    List<PlayerFollow> findByFollowerId(Long followerId);
    
    List<PlayerFollow> findByGamePlayerId(Long gamePlayerId);
    
    @Query("SELECT COUNT(f) FROM PlayerFollow f WHERE f.gamePlayer.id = ?1")
    Long countFollowersByGamePlayerId(Long gamePlayerId);
    
    @Query("SELECT COUNT(f) FROM PlayerFollow f WHERE f.follower.id = ?1")
    Long countFollowingByFollowerId(Long followerId);

    @Query(value = "SELECT gp.id as gamePlayerId, gp.username, " +
           "COUNT(f.id) as followerCount, " +
           "COALESCE(AVG(r.rating), 0) as averageRating " +
           "FROM game_players gp " +
           "LEFT JOIN player_follows f ON gp.id = f.game_player_id " +
           "LEFT JOIN reviews r ON gp.id = r.game_player_id " +
           "WHERE gp.status = 'AVAILABLE' " +
           "GROUP BY gp.id, gp.username " +
           "ORDER BY followerCount DESC, averageRating DESC " +
           "LIMIT ?1 OFFSET ?2", nativeQuery = true)
    List<Object[]> findPopularPlayers(int limit, int offset);

    @Query(value = "WITH user_followers AS (" +
           "  SELECT f2.game_player_id " +
           "  FROM player_follows f1 " +
           "  JOIN player_follows f2 ON f1.game_player_id = f2.follower_id " +
           "  WHERE f1.follower_id = ?1 " +
           "), " +
           "blocked_users AS (" +
           "  SELECT blocked_id FROM user_blocks WHERE blocker_id = ?1 " +
           "  UNION " +
           "  SELECT blocker_id FROM user_blocks WHERE blocked_id = ?1 " +
           ") " +
           "SELECT gp.id as gamePlayerId, gp.username, " +
           "COUNT(f.id) as followerCount, " +
           "COALESCE(AVG(r.rating), 0) as averageRating, " +
           "COUNT(uf.game_player_id) as commonFollowers " +
           "FROM game_players gp " +
           "LEFT JOIN player_follows f ON gp.id = f.game_player_id " +
           "LEFT JOIN reviews r ON gp.id = r.game_player_id " +
           "LEFT JOIN user_followers uf ON gp.id = uf.game_player_id " +
           "WHERE gp.user_id != ?1 " +
           "AND gp.id NOT IN (SELECT game_player_id FROM player_follows WHERE follower_id = ?1) " +
           "AND gp.user_id NOT IN (SELECT id FROM blocked_users) " +
           "AND gp.status = 'AVAILABLE' " +
           "GROUP BY gp.id, gp.username " +
           "ORDER BY commonFollowers DESC, followerCount DESC, averageRating DESC " +
           "LIMIT 10", nativeQuery = true)
    List<Object[]> findSuggestedPlayers(Long userId);
} 