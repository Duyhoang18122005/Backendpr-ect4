package com.example.backend.service;

import com.example.backend.dto.MomentDTO;
import com.example.backend.dto.CreateMomentRequest;
import com.example.backend.entity.Moment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MomentService {
    
    // Tạo moment mới
    MomentDTO createMoment(Long gamePlayerId, CreateMomentRequest request);
    
    // Lấy moment theo ID
    MomentDTO getMomentById(Long momentId);
    
    // Lấy tất cả moment của một game player
    Page<MomentDTO> getMomentsByGamePlayerId(Long gamePlayerId, Pageable pageable);
    
    // Lấy tất cả moment của user đang đăng nhập
    Page<MomentDTO> getMyMoments(Long userId, Pageable pageable);
    
    // Lấy feed moment (moment của những player mà user đang follow)
    Page<MomentDTO> getMomentFeed(Long userId, Pageable pageable);
    
    // Lấy tất cả moment
    Page<MomentDTO> getAllMoments(org.springframework.data.domain.Pageable pageable);
    
    // Cập nhật moment
    MomentDTO updateMoment(Long momentId, Long userId, CreateMomentRequest request);
    
    // Xóa moment
    void deleteMoment(Long momentId, Long userId);
    
    // Ẩn/hiện moment
    void toggleMomentVisibility(Long momentId, Long userId);
    
    // Kiểm tra xem user có phải là owner của game player không
    boolean isGamePlayerOwner(Long gamePlayerId, Long userId);
} 