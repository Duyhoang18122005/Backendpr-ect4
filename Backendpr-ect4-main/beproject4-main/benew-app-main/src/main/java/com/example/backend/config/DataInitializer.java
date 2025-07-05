package com.example.backend.config;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {
    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) {
        // Kiểm tra user admin đã tồn tại chưa
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(new BCryptPasswordEncoder().encode("Hoangpro1234@"));
            admin.setEmail("admin@example.com");
            admin.setFullName("Admin");
            admin.setPhoneNumber("0123456789");
            admin.setAddress("");
            admin.setWalletBalance(java.math.BigDecimal.ZERO);
            Set<String> roles = new HashSet<>();
            roles.add("ROLE_ADMIN");
            roles.add("ROLE_PLAYER");
            admin.setRoles(roles);
            userRepository.save(admin);
            System.out.println("Tạo tài khoản admin thành công!");
        }
    }
} 