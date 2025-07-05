package com.example.backend.service;

import com.example.backend.entity.Conversation;
import com.example.backend.entity.Message;
import com.example.backend.entity.User;
import java.util.List;

public interface MessageService {
    Message sendMessage(User sender, User receiver, String content);
    List<Message> getConversation(User user1, User user2);
    List<Message> getUnreadMessages(User user);
    long getUnreadMessageCount(User user);
    void markMessageAsRead(Long messageId);
    void markAllMessagesAsRead(User user1, User user2);
    List<Conversation> getUserConversations(User user);
    Conversation getOrCreateConversation(User user1, User user2);
    List<Message> getMessagesBySender(User sender);
    List<Message> getMessagesByReceiver(User receiver);
} 