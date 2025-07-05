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

@Service
@Transactional
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // Đường dẫn tới file JSON service account
    private static final String SERVICE_ACCOUNT_FILE = "notifigation-d7a54-firebase-adminsdk-fbsvc-b423d542ff.json";
    // Lấy projectId từ file JSON hoặc Firebase Console
    private static final String PROJECT_ID = "notifigation-d7a54";

    public NotificationService(NotificationRepository notificationRepository,
                             UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public Notification createNotification(Long userId, String title, String message,
                                        String type, String actionUrl, Long orderId) {
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
        notification.setOrderId(orderId);

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

            // 1. Lấy access token từ file JSON
            GoogleCredentials googleCredentials = GoogleCredentials
                    .fromStream(resource.getInputStream())
                    .createScoped(Collections.singleton("https://www.googleapis.com/auth/firebase.messaging"));
            
            System.out.println("[NotificationService] Credentials created successfully");
            System.out.println("[NotificationService] Using Project ID: " + PROJECT_ID);
            System.out.println("[NotificationService] Service Account Email: " + ((ServiceAccountCredentials)googleCredentials).getClientEmail());
            
            googleCredentials.refreshIfExpired();
            String accessToken = googleCredentials.getAccessToken().getTokenValue();
            System.out.println("[NotificationService] Access token obtained successfully");

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
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            System.out.println("FCM v1 response: " + response.getBody());
        } catch (Exception e) {
            System.err.println("Lỗi gửi FCM v1: " + e.getMessage());
            e.printStackTrace();
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