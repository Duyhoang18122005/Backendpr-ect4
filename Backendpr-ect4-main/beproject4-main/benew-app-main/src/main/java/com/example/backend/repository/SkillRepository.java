package com.example.backend.repository;

import com.example.backend.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findByGamePlayerId(Long gamePlayerId);
    List<Skill> findByGamePlayerIdAndName(Long gamePlayerId, String name);
    List<Skill> findByLevel(String level);
    List<Skill> findByGamePlayerIdAndLevel(Long gamePlayerId, String level);
} 