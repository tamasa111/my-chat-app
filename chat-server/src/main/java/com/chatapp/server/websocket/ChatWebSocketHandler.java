package com.chatapp.server.websocket;

import com.chatapp.common.model.ChatMessage;
import com.chatapp.common.model.FileMessage;
import com.chatapp.common.model.LoginRequest;
import com.chatapp.common.model.LoginResponse;
import com.chatapp.common.model.SystemMessage;
import com.chatapp.common.model.TextMessage;
import com.chatapp.server.service.ChatPresenceService;
import com.chatapp.server.service.MessageService;
import com.chatapp.server.service.UserService;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Controller
public class ChatWebSocketHandler {

    private final UserService userService;
    private final MessageService messageService;
    private final ChatPresenceService chatPresenceService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketHandler(
            UserService userService,
            MessageService messageService,
            ChatPresenceService chatPresenceService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.userService = userService;
        this.messageService = messageService;
        this.chatPresenceService = chatPresenceService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat")
    public void handleMessage(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor) {
        if (message instanceof LoginRequest loginRequest) {
            handleLogin(loginRequest, headerAccessor);
            return;
        }

        String sessionId = headerAccessor.getSessionId();
        String sender = sessionId == null ? null : chatPresenceService.getUsername(sessionId);
        if (sender == null || sender.isBlank()) {
            return;
        }

        if (message instanceof TextMessage textMessage) {
            handleTextMessage(sender, textMessage);
        } else if (message instanceof FileMessage fileMessage) {
            relayFileMessage(sender, fileMessage);
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String username = chatPresenceService.removeSession(event.getSessionId());
        if (username == null || chatPresenceService.isOnline(username)) {
            return;
        }
        SystemMessage leftMessage = messageService.saveSystemMessage(username + " left the chat");
        messagingTemplate.convertAndSend("/topic/messages", leftMessage);
        chatPresenceService.broadcastUserList();
    }

    private void handleLogin(LoginRequest request, SimpMessageHeaderAccessor headerAccessor) {
        boolean valid = userService.validateCredentials(request.getUsername(), request.getPassword());
        if (!valid) {
            messagingTemplate.convertAndSend("/topic/login",
                    new LoginResponse(false, request.getUsername(), "Invalid username or password"));
            return;
        }

        String sessionId = headerAccessor.getSessionId();
        if (sessionId != null) {
            chatPresenceService.registerSession(sessionId, request.getUsername());
        }

        messagingTemplate.convertAndSend("/topic/login",
                new LoginResponse(true, request.getUsername(), "Login successful"));

        List<ChatMessage> history = messageService.getHistoryForUser(request.getUsername());
        history.forEach(chatMessage -> chatPresenceService.sendHistoryMessage(request.getUsername(), chatMessage));

        SystemMessage joinedMessage = messageService.saveSystemMessage(request.getUsername() + " joined the chat");
        messagingTemplate.convertAndSend("/topic/messages", joinedMessage);
        chatPresenceService.broadcastUserList();
    }

    private void handleTextMessage(String sender, TextMessage message) {
        TextMessage storedMessage = messageService.saveTextMessage(sender, message.getContent(), message.getRecipient());
        if (storedMessage.getRecipient() == null || storedMessage.getRecipient().isBlank()) {
            messagingTemplate.convertAndSend("/topic/messages", storedMessage);
            return;
        }

        messagingTemplate.convertAndSendToUser(storedMessage.getSender(), "/queue/messages", storedMessage);
        messagingTemplate.convertAndSendToUser(storedMessage.getRecipient(), "/queue/messages", storedMessage);
    }

    private void relayFileMessage(String sender, FileMessage fileMessage) {
        FileMessage message = new FileMessage(
                sender,
                fileMessage.getRecipient(),
                fileMessage.getFileId(),
                fileMessage.getOriginalFilename(),
                fileMessage.getSize(),
                fileMessage.getContentType()
        );
        message.setTimestamp(fileMessage.getTimestamp());

        if (message.getRecipient() == null || message.getRecipient().isBlank()) {
            messagingTemplate.convertAndSend("/topic/messages", message);
            return;
        }

        messagingTemplate.convertAndSendToUser(message.getSender(), "/queue/messages", message);
        messagingTemplate.convertAndSendToUser(message.getRecipient(), "/queue/messages", message);
    }
}
