package com.example.backend.service;

import com.example.backend.entity.Skill;
import com.example.backend.entity.GamePlayer;
import com.example.backend.repository.SkillRepository;
import com.example.backend.repository.GamePlayerRepository;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.SkillException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class SkillService {
    private final SkillRepository skillRepository;
    private final GamePlayerRepository gamePlayerRepository;

    public SkillService(SkillRepository skillRepository,
                       GamePlayerRepository gamePlayerRepository) {
        this.skillRepository = skillRepository;
        this.gamePlayerRepository = gamePlayerRepository;
    }

    public Skill addSkill(Long gamePlayerId, String name, String level,
                         String description, String certification, String proof) {
        GamePlayer gamePlayer = gamePlayerRepository.findById(gamePlayerId)
                .orElseThrow(() -> new ResourceNotFoundException("Game player not found"));

        validateLevel(level);

        // Check if skill already exists for this player
        List<Skill> existingSkills = skillRepository.findByGamePlayerIdAndName(gamePlayerId, name);
        if (!existingSkills.isEmpty()) {
            throw new SkillException("Skill already exists for this player");
        }

        Skill skill = new Skill();
        skill.setGamePlayer(gamePlayer);
        skill.setName(name);
        skill.setLevel(level);
        skill.setDescription(description);
        skill.setCertification(certification);
        skill.setProof(proof);

        return skillRepository.save(skill);
    }

    public Skill updateSkill(Long skillId, String name, String level,
                           String description, String certification, String proof) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found"));

        validateLevel(level);

        skill.setName(name);
        skill.setLevel(level);
        skill.setDescription(description);
        skill.setCertification(certification);
        skill.setProof(proof);

        return skillRepository.save(skill);
    }

    public void removeSkill(Long skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found"));
        skillRepository.delete(skill);
    }

    public List<Skill> getGamePlayerSkills(Long gamePlayerId) {
        return skillRepository.findByGamePlayerId(gamePlayerId);
    }

    public List<Skill> getSkillsByLevel(String level) {
        validateLevel(level);
        return skillRepository.findByLevel(level);
    }

    public List<Skill> getGamePlayerSkillsByLevel(Long gamePlayerId, String level) {
        validateLevel(level);
        return skillRepository.findByGamePlayerIdAndLevel(gamePlayerId, level);
    }

    private void validateLevel(String level) {
        if (!Arrays.asList("BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT").contains(level)) {
            throw new IllegalArgumentException("Invalid skill level");
        }
    }
} 