package com.example.backend.service;

import com.example.backend.entity.Notification;
import com.example.backend.entity.User;
import com.example.backend.repository.NotificationRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.ClassPathResource;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.FileInputStream;
import java.util.Collections;
import java.io.File;
import java.io.FileNotFoundException;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.InputStream;

@Service
@Transactional
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // Đường dẫn tới file JSON service account
    private static final String SERVICE_ACCOUNT_FILE = "notifigation-d7a54-firebase-adminsdk-fbsvc-e812eeeadd.json";
    // Lấy projectId từ file JSON hoặc Firebase Console
    private static final String PROJECT_ID = "notifigation-d7a54";

    public NotificationService(NotificationRepository notificationRepository,
                             UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public Notification createNotification(Long userId, String title, String message,
                                        String type, String actionUrl, String relatedId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title != null ? title : "");
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setActionUrl(actionUrl);
        notification.setStatus("ACTIVE");
        notification.setOrderId(relatedId != null ? Long.valueOf(relatedId) : null);

        notification = notificationRepository.save(notification);

        // Gửi push notification nếu user có deviceToken
        if (user.getDeviceToken() != null && !user.getDeviceToken().isEmpty()) {
            sendPushNotification(
                user.getDeviceToken(),
                notification.getTitle(),
                notification.getMessage(),
                null // Có thể truyền imageUrl nếu muốn
            );
        }

        return notification;
    }

    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    public void deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        notification.setStatus("DELETED");
        notificationRepository.save(notification);
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdAndStatus(userId, "ACTIVE");
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsRead(userId, false);
    }

    public List<Notification> getNotificationsByType(Long userId, String type) {
        return notificationRepository.findByUserIdAndType(userId, type);
    }

    public List<Notification> getRecentNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void sendPushNotification(String deviceToken, String title, String body, String imageUrl) {
        try {
            System.out.println("[NotificationService] BẮT ĐẦU gửi push notification cho deviceToken=" + deviceToken);
            
            // Đọc file từ resources
            ClassPathResource resource = new ClassPathResource(SERVICE_ACCOUNT_FILE);
            if (!resource.exists()) {
                throw new FileNotFoundException("Service account file not found in resources: " + SERVICE_ACCOUNT_FILE);
            }
            System.out.println("[NotificationService] Service account file exists in resources");

            // Log file content for debugging (only first few characters for security)
            try (InputStream is = resource.getInputStream()) {
                byte[] bytes = is.readAllBytes();
                String content = new String(bytes);
                System.out.println("[NotificationService] File size: " + bytes.length + " bytes");
                System.out.println("[NotificationService] File starts with: " + content.substring(0, Math.min(100, content.length())));
                
                // Check if file has BOM
                if (bytes.length > 3 && bytes[0] == (byte)0xEF && bytes[1] == (byte)0xBB && bytes[2] == (byte)0xBF) {
                    System.out.println("[NotificationService] WARNING: File has UTF-8 BOM, this might cause issues!");
                }
                
                // Re-read the file for credentials
                try (InputStream is2 = resource.getInputStream()) {
            // 1. Lấy access token từ file JSON
            GoogleCredentials googleCredentials = GoogleCredentials
                            .fromStream(is2)
                    .createScoped(Collections.singleton("https://www.googleapis.com/auth/firebase.messaging"));
            
            System.out.println("[NotificationService] Credentials created successfully");
            System.out.println("[NotificationService] Using Project ID: " + PROJECT_ID);
                    
                    if (googleCredentials instanceof ServiceAccountCredentials) {
                        ServiceAccountCredentials serviceAccount = (ServiceAccountCredentials) googleCredentials;
                        System.out.println("[NotificationService] Service Account Email: " + serviceAccount.getClientEmail());
                        System.out.println("[NotificationService] Private Key ID: " + serviceAccount.getPrivateKeyId());
                        System.out.println("[NotificationService] Project ID from credentials: " + serviceAccount.getProjectId());
                        
                        // Verify project ID matches
                        if (!PROJECT_ID.equals(serviceAccount.getProjectId())) {
                            System.out.println("[NotificationService] WARNING: Project ID mismatch! Expected: " + PROJECT_ID + ", Got: " + serviceAccount.getProjectId());
                        }
                    }
            
            googleCredentials.refreshIfExpired();
            String accessToken = googleCredentials.getAccessToken().getTokenValue();
            System.out.println("[NotificationService] Access token obtained successfully");
                    System.out.println("[NotificationService] Access token length: " + accessToken.length());

            // 2. Tạo payload
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("{")
                .append("\"message\":{")
                .append("\"token\":\"").append(deviceToken).append("\",")
                .append("\"notification\":{")
                .append("\"title\":\"").append(title.replace("\"", "\\\"")).append("\",")
                .append("\"body\":\"").append(body.replace("\"", "\\\"")).append("\"");
            if (imageUrl != null && !imageUrl.isEmpty()) {
                messageBuilder.append(",\"image\":\"").append(imageUrl).append("\"");
            }
            messageBuilder.append("}");
            messageBuilder.append("}");
            messageBuilder.append("}");
            String message = messageBuilder.toString();

            // 3. Gửi request
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(message, headers);
            RestTemplate restTemplate = new RestTemplate();

            String url = "https://fcm.googleapis.com/v1/projects/" + PROJECT_ID + "/messages:send";
                    System.out.println("[NotificationService] Sending request to: " + url);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            System.out.println("FCM v1 response: " + response.getBody());
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi FCM v1: " + e.getMessage());
            e.printStackTrace();
            
            // Additional debugging information
            if (e.getMessage().contains("Invalid JWT Signature")) {
                System.err.println("[DEBUG] JWT Signature error detected. Possible causes:");
                System.err.println("1. Service account JSON file is corrupted");
                System.err.println("2. Private key is invalid or expired");
                System.err.println("3. Service account has been disabled");
                System.err.println("4. File encoding issues (UTF-8 BOM)");
                System.err.println("5. Project ID mismatch");
            }
        }
    }

    public void updateDeviceToken(Long userId, String deviceToken) {
        System.out.println("[NotificationService] updateDeviceToken: userId=" + userId + ", deviceToken=" + deviceToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        System.out.println("[NotificationService] Found user: " + user.getUsername());
        user.setDeviceToken(deviceToken);
        userRepository.save(user);
        System.out.println("[NotificationService] Device token updated successfully for userId=" + userId);
    }
} 