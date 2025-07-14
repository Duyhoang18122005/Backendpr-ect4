package com.example.backend.service;

import com.example.backend.entity.Conversation;
import com.example.backend.entity.Message;
import com.example.backend.entity.User;
import com.example.backend.repository.ConversationRepository;
import com.example.backend.repository.MessageRepository;
import com.example.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final NotificationService notificationService;

    @Autowired
    public MessageServiceImpl(MessageRepository messageRepository,
                             ConversationRepository conversationRepository,
                             NotificationService notificationService) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public Message sendMessage(User sender, User receiver, String content) {
        System.out.println("[MessageServiceImpl] BẮT ĐẦU sendMessage: sender=" + sender.getId() + ", receiver=" + receiver.getId());
        
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        
        // Update or create conversation
        System.out.println("[MessageServiceImpl] Tìm hoặc tạo conversation");
        Conversation conversation = getOrCreateConversation(sender, receiver);
        System.out.println("[MessageServiceImpl] Conversation ID: " + conversation.getId());
        
        conversation.setLastMessageContent(content);
        conversation.setLastMessageTime(message.getTimestamp());
        if (!sender.equals(conversation.getUser1())) {
            conversation.setUnreadCount(conversation.getUnreadCount() + 1);
        }
        System.out.println("[MessageServiceImpl] Lưu conversation");
        conversationRepository.save(conversation);
        System.out.println("[MessageServiceImpl] Đã lưu conversation");

        // Gửi notification cho người nhận khi có tin nhắn mới
        notificationService.createNotification(
            receiver.getId(),
            "Bạn có tin nhắn mới!",
            sender.getUsername() + " vừa gửi cho bạn một tin nhắn: '" + content + "'",
            "message",
            null,
            null
        );

        System.out.println("[MessageServiceImpl] Lưu message");
        Message savedMessage = messageRepository.save(message);
        System.out.println("[MessageServiceImpl] Đã lưu message với ID: " + savedMessage.getId());
        
        return savedMessage;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getConversation(User user1, User user2) {
        return messageRepository.findConversation(user1, user2);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getUnreadMessages(User user) {
        return messageRepository.findUnreadMessages(user);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadMessageCount(User user) {
        return messageRepository.countUnreadMessages(user);
    }

    @Override
    @Transactional
    public void markMessageAsRead(Long messageId) {
        messageRepository.findById(messageId).ifPresent(message -> {
            message.setRead(true);
            messageRepository.save(message);
            
            // Update conversation unread count
            Conversation conversation = getOrCreateConversation(message.getSender(), message.getReceiver());
            if (conversation.getUnreadCount() > 0) {
                conversation.setUnreadCount(conversation.getUnreadCount() - 1);
                conversationRepository.save(conversation);
            }
        });
    }

    @Override
    @Transactional
    public void markAllMessagesAsRead(User user1, User user2) {
        List<Message> messages = messageRepository.findConversation(user1, user2);
        messages.forEach(message -> {
            if (message.getReceiver().equals(user1)) {
                message.setRead(true);
            }
        });
        messageRepository.saveAll(messages);
        
        // Reset conversation unread count
        Conversation conversation = getOrCreateConversation(user1, user2);
        conversation.setUnreadCount(0);
        conversationRepository.save(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conversation> getUserConversations(User user) {
        return conversationRepository.findAllByUser(user);
    }

    @Override
    @Transactional
    public Conversation getOrCreateConversation(User user1, User user2) {
        Conversation conversation = conversationRepository.findConversationBetweenUsers(user1, user2);
        if (conversation == null) {
            conversation = new Conversation();
            conversation.setUser1(user1);
            conversation.setUser2(user2);
            conversation.setLastMessageTime(LocalDateTime.now());
            conversation = conversationRepository.save(conversation);
        }
        return conversation;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getMessagesBySender(User sender) {
        return messageRepository.findBySender(sender);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getMessagesByReceiver(User receiver) {
        return messageRepository.findByReceiver(receiver);
    }
} 