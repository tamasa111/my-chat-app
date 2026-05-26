package com.chatapp.server.service;

import com.chatapp.common.model.ChatMessage;
import com.chatapp.common.model.UserListMessage;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatPresenceService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    private final ConcurrentHashMap<String, String> sessionUsers = new ConcurrentHashMap<>();

    public ChatPresenceService(SimpMessagingTemplate messagingTemplate, UserService userService) {
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
    }

    public void registerSession(String sessionId, String username) {
        sessionUsers.put(sessionId, username);
    }

    public String removeSession(String sessionId) {
        return sessionUsers.remove(sessionId);
    }

    public String getUsername(String sessionId) {
        return sessionUsers.get(sessionId);
    }

    public Set<String> getOnlineUsers() {
        return Set.copyOf(sessionUsers.values());
    }

    public boolean isOnline(String username) {
        return sessionUsers.containsValue(username);
    }

    public void broadcastUserList() {
        List<UserListMessage.UserStatus> statuses = userService.getUserStatuses(getOnlineUsers());
        messagingTemplate.convertAndSend("/topic/users", new UserListMessage(statuses));
    }

    public void sendHistoryMessage(String username, ChatMessage message) {
        messagingTemplate.convertAndSend("/topic/user/" + username + "/history", message);
    }
}
