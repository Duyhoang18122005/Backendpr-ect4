package com.example.backend.service;

import com.example.backend.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.io.IOException;

public interface UserService extends UserDetailsService {
    User save(User user);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    User findByUsername(String username);
    User findById(Long id);
    User update(Long id, String fullName, LocalDate dateOfBirth, String phoneNumber, 
               String address, String bio, String gender);
    User updateAvatar(Long id, MultipartFile avatarFile) throws IOException;
    User updateProfileImage(Long id, MultipartFile profileImageFile) throws IOException;
    void deleteAvatar(Long id) throws IOException;
    void deleteProfileImage(Long id) throws IOException;
    long countUsers();
    double getUserGrowthPercentComparedToLastWeek();
    java.util.List<User> findAll();
    void deleteById(Long id);
    void updateUserRoles(Long userId, java.util.Set<String> roles);
    void setUserOffline(Long userId);
    java.util.List<User> findRecentUsers();
} 