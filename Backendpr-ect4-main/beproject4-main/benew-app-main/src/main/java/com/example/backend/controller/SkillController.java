package com.example.backend.controller;

import com.example.backend.entity.Skill;
import com.example.backend.service.SkillService;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.SkillException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Skill", description = "Skill management APIs")
public class SkillController {
    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @Operation(summary = "Add a new skill")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Skill> addSkill(@Valid @RequestBody SkillRequest request) {
        try {
            Skill skill = skillService.addSkill(
                    request.getGamePlayerId(),
                    request.getName(),
                    request.getLevel(),
                    request.getDescription(),
                    request.getCertification(),
                    request.getProof()
            );
            return ResponseEntity.ok(skill);
        } catch (SkillException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Update a skill")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Skill> updateSkill(
            @Parameter(description = "Skill ID") @PathVariable Long id,
            @Valid @RequestBody SkillRequest request) {
        try {
            Skill skill = skillService.updateSkill(
                    id,
                    request.getName(),
                    request.getLevel(),
                    request.getDescription(),
                    request.getCertification(),
                    request.getProof()
            );
            return ResponseEntity.ok(skill);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Remove a skill")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeSkill(
            @Parameter(description = "Skill ID") @PathVariable Long id) {
        try {
            skillService.removeSkill(id);
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get game player skills")
    @GetMapping("/game-player/{gamePlayerId}")
    public ResponseEntity<List<Skill>> getGamePlayerSkills(
            @Parameter(description = "Game player ID") @PathVariable Long gamePlayerId) {
        return ResponseEntity.ok(skillService.getGamePlayerSkills(gamePlayerId));
    }

    @Operation(summary = "Get skills by level")
    @GetMapping("/level/{level}")
    public ResponseEntity<List<Skill>> getSkillsByLevel(
            @Parameter(description = "Skill level") @PathVariable String level) {
        return ResponseEntity.ok(skillService.getSkillsByLevel(level));
    }

    @Operation(summary = "Get game player skills by level")
    @GetMapping("/game-player/{gamePlayerId}/level/{level}")
    public ResponseEntity<List<Skill>> getGamePlayerSkillsByLevel(
            @Parameter(description = "Game player ID") @PathVariable Long gamePlayerId,
            @Parameter(description = "Skill level") @PathVariable String level) {
        return ResponseEntity.ok(skillService.getGamePlayerSkillsByLevel(gamePlayerId, level));
    }
}

@Data
class SkillRequest {
    @NotNull(message = "Game player ID is required")
    private Long gamePlayerId;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Level is required")
    private String level;

    @NotBlank(message = "Description is required")
    private String description;

    private String certification;

    private String proof;
} 