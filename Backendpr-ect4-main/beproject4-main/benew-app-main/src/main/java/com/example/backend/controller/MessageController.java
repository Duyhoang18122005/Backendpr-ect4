package com.example.backend.controller;

import com.example.backend.entity.Message;
import com.example.backend.entity.User;
import com.example.backend.entity.GamePlayer;
import com.example.backend.service.MessageService;
import com.example.backend.service.UserService;
import com.example.backend.service.GamePlayerService;
import lombok.Data;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "http://localhost:3000")
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;
    private final GamePlayerService gamePlayerService;

    public MessageController(MessageService messageService, UserService userService, GamePlayerService gamePlayerService) {
        this.messageService = messageService;
        this.userService = userService;
        this.gamePlayerService = gamePlayerService;
    }

    @PostMapping("/send/{userId}")
    public ResponseEntity<?> sendMessage(
            @PathVariable Long userId,
            @RequestBody MessageRequest request,
            Authentication authentication) {
        User sender = userService.findByUsername(authentication.getName());
        User receiver = userService.findById(userId);
        if (receiver == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        Message message = messageService.sendMessage(sender, receiver, request.getContent());
        String senderName = getDisplayName(sender);
        String receiverName = getDisplayName(receiver);
        DetailMessageDTO response = new DetailMessageDTO(
            message.getContent(),
            message.getSender().getId(),
            senderName,
            message.getReceiver().getId(),
            receiverName,
            message.getTimestamp(),
            message.isRead()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversation/{userId}")
    public ResponseEntity<?> getConversation(
            @PathVariable Long userId,
            Authentication authentication) {
        User currentUser = userService.findByUsername(authentication.getName());
        User otherUser = userService.findById(userId);
        var conversation = messageService.getOrCreateConversation(currentUser, otherUser);
        List<Message> messages = messageService.getConversation(currentUser, otherUser);
        List<DetailMessageDTO> detailList = messages.stream()
            .map(m -> new DetailMessageDTO(
                m.getContent(),
                m.getSender().getId(),
                getDisplayName(m.getSender()),
                m.getReceiver().getId(),
                getDisplayName(m.getReceiver()),
                m.getTimestamp(),
                m.isRead()
            ))
            .toList();
        String otherName = getDisplayName(otherUser);
        ConversationDetailDTO result = new ConversationDetailDTO(conversation.getId(), otherName, detailList);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadMessages(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        List<Message> unreadMessages = messageService.getUnreadMessages(user);
        return ResponseEntity.ok(unreadMessages);
    }

    @GetMapping("/unread/count")
    public ResponseEntity<?> getUnreadMessageCount(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        long count = messageService.getUnreadMessageCount(user);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/read/{messageId}")
    public ResponseEntity<?> markMessageAsRead(@PathVariable Long messageId) {
        messageService.markMessageAsRead(messageId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all/{userId}")
    public ResponseEntity<?> markAllMessagesAsRead(
            @PathVariable Long userId,
            Authentication authentication) {
        User currentUser = userService.findByUsername(authentication.getName());
        User otherUser = userService.findById(userId);
        
        messageService.markAllMessagesAsRead(currentUser, otherUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/all-conversations")
    public ResponseEntity<?> getAllConversations(Authentication authentication) {
        User currentUser = userService.findByUsername(authentication.getName());
        System.out.println("[DEBUG] Current user: " + currentUser.getId() + " - " + currentUser.getUsername());
        List<Message> sentMessages = messageService.getMessagesBySender(currentUser);
        List<Message> receivedMessages = messageService.getMessagesByReceiver(currentUser);
        System.out.println("[DEBUG] Sent messages: " + sentMessages.size());
        System.out.println("[DEBUG] Received messages: " + receivedMessages.size());
        Set<Long> chatUserIds = new HashSet<>();
        for (Message message : sentMessages) {
            chatUserIds.add(message.getReceiver().getId());
        }
        for (Message message : receivedMessages) {
            chatUserIds.add(message.getSender().getId());
        }
        chatUserIds.remove(currentUser.getId());
        System.out.println("[DEBUG] Chat user ids: " + chatUserIds);
        Map<Long, List<DetailMessageDTO>> conversationMap = new HashMap<>();
        Map<Long, String> nameMap = new HashMap<>();
        for (Long otherUserId : chatUserIds) {
            User otherUser = userService.findById(otherUserId);
            if (otherUser != null) {
                List<Message> messages = messageService.getConversation(currentUser, otherUser);
                System.out.println("[DEBUG] Messages with user " + otherUserId + ": " + messages.size());
                List<DetailMessageDTO> detailMessages = messages.stream()
                    .map(m -> new DetailMessageDTO(
                        m.getContent(),
                        m.getSender().getId(),
                        getDisplayName(m.getSender()),
                        m.getReceiver().getId(),
                        getDisplayName(m.getReceiver()),
                        m.getTimestamp(),
                        m.isRead()
                    ))
                    .toList();
                conversationMap.put(otherUserId, detailMessages);
                List<GamePlayer> players = gamePlayerService.findByUserId(otherUserId);
                String otherName;
                if (players != null && !players.isEmpty()) {
                    otherName = players.get(0).getUsername();
                } else {
                    otherName = (otherUser.getFullName() != null && !otherUser.getFullName().isEmpty())
                        ? otherUser.getFullName()
                        : otherUser.getUsername();
                }
                nameMap.put(otherUserId, otherName);
            }
        }
        List<ConversationDetailDTO> result = conversationMap.entrySet().stream()
            .map(entry -> new ConversationDetailDTO(entry.getKey(), nameMap.get(entry.getKey()), entry.getValue()))
            .toList();
        System.out.println("[DEBUG] Result conversations: " + result.size());
        return ResponseEntity.ok(result);
    }

    private String getDisplayName(User user) {
        List<GamePlayer> players = gamePlayerService.findByUserId(user.getId());
        if (players != null && !players.isEmpty()) {
            return players.get(0).getUsername();
        }
        return (user.getFullName() != null && !user.getFullName().isEmpty())
            ? user.getFullName()
            : user.getUsername();
    }
}

@Data
class MessageRequest {
    private String content;
}

@Data
@AllArgsConstructor
class SimpleMessageDTO {
    private boolean fromMe;
    private String content;
}

@Data
@AllArgsConstructor
class ConversationDetailDTO {
    private Long conversationId;
    private String otherName;
    private List<DetailMessageDTO> messages;
}

@Data
@AllArgsConstructor
class DetailMessageDTO {
    private String content;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    private LocalDateTime timestamp;
    private boolean read;
} 