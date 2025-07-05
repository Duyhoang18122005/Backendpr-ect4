package com.example.backend.controller;

import com.example.backend.entity.User;
import com.example.backend.security.JwtTokenUtil;
import com.example.backend.service.UserService;
import com.example.backend.service.NotificationService;
import com.example.backend.service.PasswordResetService;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserService userService;
    private final NotificationService notificationService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthenticationManager authenticationManager,
                         JwtTokenUtil jwtTokenUtil,
                         UserService userService,
                         NotificationService notificationService,
                         PasswordResetService passwordResetService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userService = userService;
        this.notificationService = notificationService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            // Validate username
            if (registerRequest.getUsername() == null || registerRequest.getUsername().length() < 3) {
                return ResponseEntity.badRequest().body("Tên đăng nhập phải có ít nhất 3 ký tự");
            }

            // Validate password
            if (registerRequest.getPassword() == null || registerRequest.getPassword().length() < 8) {
                return ResponseEntity.badRequest().body("Mật khẩu phải có ít nhất 8 ký tự");
            }
            if (!registerRequest.getPassword().matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$")) {
                return ResponseEntity.badRequest().body("Mật khẩu phải chứa ít nhất 1 chữ số, 1 chữ thường, 1 chữ hoa và 1 ký tự đặc biệt");
            }

            // Validate email
            if (registerRequest.getEmail() == null || !registerRequest.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                return ResponseEntity.badRequest().body("Email không hợp lệ");
            }

            // Check existing username/email
        if (userService.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity.badRequest().body("Tên đăng nhập đã tồn tại");
        }
        if (userService.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest().body("Email đã tồn tại");
        }

            // Create user
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(registerRequest.getPassword());
        user.setEmail(registerRequest.getEmail());
        user.setFullName(registerRequest.getFullName());
        user.setDateOfBirth(registerRequest.getDateOfBirth());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setAddress(registerRequest.getAddress());
        user.setGender(registerRequest.getGender());
            user.setCoin(0L); // Initialize coin balance
        user.getRoles().add("ROLE_USER");
            user.setEnabled(true);
            user.setAccountNonExpired(true);
            user.setCredentialsNonExpired(true);
            user.setAccountNonLocked(true);

        userService.save(user);

            // Gửi notification chào mừng khi đăng ký
            notificationService.createNotification(
                user.getId(),
                "Chào mừng đến với PlayerDuo!",
                "Cảm ơn bạn đã đăng ký tài khoản.",
                "welcome",
                null,
                null
            );

            return ResponseEntity.ok(Map.of(
                "message", "Đăng ký thành công",
                "username", user.getUsername(),
                "email", user.getEmail()
            ));
        } catch (Exception e) {
            logger.error("Lỗi khi đăng ký: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Lỗi khi đăng ký: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Validate input
            if (loginRequest.getUsername() == null || loginRequest.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Tên đăng nhập không được để trống");
            }
            if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Mật khẩu không được để trống");
            }

            // Authenticate
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtTokenUtil.generateToken(userDetails);
            String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

            // Get user info
            User user = userService.findByUsername(loginRequest.getUsername());

            return ResponseEntity.ok(Map.of(
                "token", token,
                "refreshToken", refreshToken,
                "username", user.getUsername(),
                "email", user.getEmail(),
                "roles", user.getRoles(),
                "coin", user.getCoin(),
                "id", user.getId()
            ));
        } catch (BadCredentialsException e) {
            logger.warn("Đăng nhập thất bại cho user: {}", loginRequest.getUsername());
            return ResponseEntity.status(401).body("Tên đăng nhập hoặc mật khẩu không đúng");
        } catch (Exception e) {
            logger.error("Lỗi khi đăng nhập: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Lỗi khi đăng nhập: " + e.getMessage());
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            String username = jwtTokenUtil.getUsernameFromToken(request.getRefreshToken());
            UserDetails userDetails = userService.loadUserByUsername(username);
            
            if (jwtTokenUtil.validateToken(request.getRefreshToken(), userDetails)) {
                String newToken = jwtTokenUtil.generateToken(userDetails);
                String newRefreshToken = jwtTokenUtil.generateRefreshToken(userDetails);
                
                return ResponseEntity.ok(Map.of(
                    "token", newToken,
                    "refreshToken", newRefreshToken
                ));
            }
            
            return ResponseEntity.status(401).body("Refresh token không hợp lệ");
        } catch (Exception e) {
            logger.error("Lỗi khi refresh token: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Lỗi khi refresh token: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication) {
        if (authentication == null) return ResponseEntity.ok().build();
        User user = userService.findByUsername(authentication.getName());
        userService.setUserOffline(user.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateUser(
            @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        User currentUser = userService.findByUsername(authentication.getName());
        User updatedUser = userService.update(
            currentUser.getId(),
            request.getFullName(),
            request.getDateOfBirth(),
            request.getPhoneNumber(),
            request.getAddress(),
            request.getBio(),
            request.getGender()
        );
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/update/avatar")
    public ResponseEntity<?> updateAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        User currentUser = userService.findByUsername(authentication.getName());
        User updatedUser = userService.updateAvatar(currentUser.getId(), file);
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/update/profile-image")
    public ResponseEntity<?> updateProfileImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        User currentUser = userService.findByUsername(authentication.getName());
        User updatedUser = userService.updateProfileImage(currentUser.getId(), file);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/delete/avatar")
    public ResponseEntity<?> deleteAvatar(Authentication authentication) throws IOException {
        User currentUser = userService.findByUsername(authentication.getName());
        userService.deleteAvatar(currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete/profile-image")
    public ResponseEntity<?> deleteProfileImage(Authentication authentication) throws IOException {
        User currentUser = userService.findByUsername(authentication.getName());
        userService.deleteProfileImage(currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/device-token")
    public ResponseEntity<?> saveDeviceToken(@RequestBody DeviceTokenRequest request, Authentication authentication) {
        System.out.println("[AuthController] ====== START saveDeviceToken ======");
        System.out.println("[AuthController] Request body: " + request);
        System.out.println("[AuthController] Authentication: " + authentication);
        System.out.println("[AuthController] Authentication name: " + authentication.getName());
        System.out.println("[AuthController] Authentication authorities: " + authentication.getAuthorities());
        
        try {
            if (request.getDeviceToken() == null || request.getDeviceToken().trim().isEmpty()) {
                System.out.println("[AuthController] Device token is null or empty");
                System.out.println("[AuthController] ====== END saveDeviceToken with error ======");
                return ResponseEntity.badRequest().body("Token thiết bị không được để trống");
            }

            User user = userService.findByUsername(authentication.getName());
            System.out.println("[AuthController] Found user: " + user);
            System.out.println("[AuthController] Current device token: " + user.getDeviceToken());
            System.out.println("[AuthController] New device token: " + request.getDeviceToken().trim());
            
            user.setDeviceToken(request.getDeviceToken().trim());
            userService.save(user);
            System.out.println("[AuthController] Device token updated successfully");
            System.out.println("[AuthController] ====== END saveDeviceToken ======");
            
            return ResponseEntity.ok(Map.of(
                "message", "Cập nhật token thiết bị thành công",
                "deviceToken", user.getDeviceToken()
            ));
        } catch (Exception e) {
            System.out.println("[AuthController] Exception: " + e.getMessage());
            e.printStackTrace();
            System.out.println("[AuthController] ====== END saveDeviceToken with error ======");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Không thể cập nhật token thiết bị: " + e.getMessage()));
        }
    }

    @PostMapping("/update/avatar-db")
    public ResponseEntity<?> updateAvatarDb(@RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
        User currentUser = userService.findByUsername(authentication.getName());
        currentUser.setAvatarData(file.getBytes());
        userService.save(currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/avatar/{userId}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long userId) {
        User user = userService.findById(userId);
        byte[] image = user.getAvatarData();
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(image);
    }

    @PostMapping("/update/profile-image-db")
    public ResponseEntity<?> updateProfileImageDb(@RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
        User currentUser = userService.findByUsername(authentication.getName());
        currentUser.setProfileImageData(file.getBytes());
        userService.save(currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/profile-image/{userId}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getProfileImage(@PathVariable Long userId) {
        User user = userService.findById(userId);
        byte[] image = user.getProfileImageData();
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(image);
    }

    @PostMapping("/update/avatar-from-url")
    public ResponseEntity<?> updateAvatarFromUrl(@RequestBody ImageUrlRequest request, Authentication authentication) throws IOException {
        User currentUser = userService.findByUsername(authentication.getName());
        URL url = new URL(request.getUrl());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = url.openStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = is.read(buffer)) > 0) {
                baos.write(buffer, 0, n);
            }
        }
        currentUser.setAvatarData(baos.toByteArray());
        userService.save(currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/update/profile-image-from-url")
    public ResponseEntity<?> updateProfileImageFromUrl(@RequestBody ImageUrlRequest request, Authentication authentication) throws IOException {
        User currentUser = userService.findByUsername(authentication.getName());
        URL url = new URL(request.getUrl());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = url.openStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = is.read(buffer)) > 0) {
                baos.write(buffer, 0, n);
            }
        }
        currentUser.setProfileImageData(baos.toByteArray());
        userService.save(currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/update/avatar-from-gallery")
    public ResponseEntity<?> updateAvatarFromGallery(@RequestBody Map<String, Object> request, Authentication authentication) {
        User currentUser = userService.findByUsername(authentication.getName());
        Object imageUrlObj = request.get("imageUrl");
        if (imageUrlObj == null) {
            return ResponseEntity.badRequest().body("imageUrl is required");
        }
        String imageUrl = imageUrlObj.toString();
        currentUser.setAvatarUrl(imageUrl);
        userService.save(currentUser);
        return ResponseEntity.ok(Map.of("avatarUrl", imageUrl));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body("Email is required");
        }
        passwordResetService.createAndSendResetToken(email);
        return ResponseEntity.ok(Map.of("message", "Nếu email tồn tại, một liên kết đặt lại mật khẩu đã được gửi."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");
        if (token == null || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body("Token và mật khẩu mới là bắt buộc");
        }
        boolean result = passwordResetService.resetPassword(token, newPassword);
        if (result) {
            return ResponseEntity.ok(Map.of("message", "Đặt lại mật khẩu thành công!"));
        } else {
            return ResponseEntity.badRequest().body("Token không hợp lệ hoặc đã hết hạn");
        }
    }

    @PostMapping("/change-password")
    @Transactional
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request, Authentication authentication) {
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");
        if (oldPassword == null || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body("Mật khẩu cũ và mật khẩu mới là bắt buộc");
        }
        User user = userService.findByUsername(authentication.getName());
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        if (!encoder.matches(oldPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body("Mật khẩu cũ không đúng");
        }
        user.setPassword(encoder.encode(newPassword));
        userService.save(user);
        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công!"));
    }
}

@Data
class LoginRequest {
    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String username;
    
    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}

@Data
class RegisterRequest {
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, message = "Tên đăng nhập phải có ít nhất 3 ký tự")
    private String username;
    
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$", 
             message = "Mật khẩu phải chứa ít nhất 1 chữ số, 1 chữ thường, 1 chữ hoa và 1 ký tự đặc biệt")
    private String password;
    
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
    
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;
    
    private LocalDate dateOfBirth;
    
    @Pattern(regexp = "^\\d{10}$", message = "Số điện thoại phải có 10 chữ số")
    private String phoneNumber;
    
    private String address;
    
    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Giới tính không hợp lệ")
    private String gender;
}

@Data
class RefreshTokenRequest {
    @NotBlank(message = "Refresh token không được để trống")
    private String refreshToken;
}

@Data
class UpdateUserRequest {
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;
    
    private LocalDate dateOfBirth;
    
    @Pattern(regexp = "^\\d{10}$", message = "Số điện thoại phải có 10 chữ số")
    private String phoneNumber;
    
    private String address;
    
    private String bio;
    
    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Giới tính không hợp lệ")
    private String gender;
}

@Data
class JwtResponse {
    private String token;

    public JwtResponse(String token) {
        this.token = token;
    }
}

@Data
class ImageUrlRequest {
    private String url;
} 