package com.example.backend.repository;

import com.example.backend.entity.MomentImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MomentImageRepository extends JpaRepository<MomentImage, Long> {
    
    // Lấy tất cả ảnh của một moment
    List<MomentImage> findByMomentIdOrderByDisplayOrderAsc(Long momentId);
    
    // Xóa tất cả ảnh của một moment
    void deleteByMomentId(Long momentId);
} 