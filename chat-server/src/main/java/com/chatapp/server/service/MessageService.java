package com.chatapp.server.service;

import com.chatapp.common.model.ChatMessage;
import com.chatapp.common.model.FileMessage;
import com.chatapp.common.model.SystemMessage;
import com.chatapp.common.model.TextMessage;
import com.chatapp.server.model.Message;
import com.chatapp.server.model.MessageType;
import com.chatapp.server.repository.MessageRepository;
import java.time.Duration;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService {

    public static final Duration HISTORY_WINDOW = Duration.ofHours(24);
    public static final Duration RETENTION_PERIOD = Duration.ofDays(7);

    private final MessageRepository messageRepository;
    private final FileStorageService fileStorageService;

    public MessageService(MessageRepository messageRepository, FileStorageService fileStorageService) {
        this.messageRepository = messageRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public TextMessage saveTextMessage(String sender, String content, String recipient) {
        Message entity = new Message();
        entity.setSender(sender);
        entity.setContent(content);
        entity.setRecipient(isBlank(recipient) ? null : recipient);
        entity.setMessageType(MessageType.TEXT);
        entity = messageRepository.save(entity);
        return toTextMessage(entity);
    }

    @Transactional
    public FileMessage saveFileMessage(String sender, String recipient, FileStorageService.StoredFile storedFile) {
        Message entity = new Message();
        entity.setSender(sender);
        entity.setRecipient(isBlank(recipient) ? null : recipient);
        entity.setMessageType(MessageType.FILE);
        entity.setFileId(storedFile.fileId());
        entity.setOriginalFilename(storedFile.originalFilename());
        entity.setSize(storedFile.size());
        entity.setContentType(storedFile.contentType());
        entity.setStoragePath(storedFile.path().toString());
        entity = messageRepository.save(entity);
        return toFileMessage(entity);
    }

    @Transactional
    public SystemMessage saveSystemMessage(String content) {
        Message entity = new Message();
        entity.setSender("system");
        entity.setContent(content);
        entity.setMessageType(MessageType.SYSTEM);
        entity = messageRepository.save(entity);
        return toSystemMessage(entity);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getHistoryForUser(String username) {
        return messageRepository.findAllVisibleHistory(username, historySince()).stream()
                .map(this::toChatMessage)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getGroupHistory() {
        return messageRepository.findGroupHistory(historySince()).stream()
                .map(this::toChatMessage)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getPeerHistory(String username) {
        return messageRepository.findPeerHistory(username, historySince()).stream()
                .map(this::toChatMessage)
                .toList();
    }

    @Transactional(readOnly = true)
    public Message findFile(String fileId) {
        return messageRepository.findByFileId(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
    }

    public int getRetentionDays() {
        return (int) RETENTION_PERIOD.toDays();
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredMessages() {
        long cutoff = System.currentTimeMillis() - RETENTION_PERIOD.toMillis();
        List<Message> expiredMessages = messageRepository.findExpired(cutoff);
        expiredMessages.stream()
                .map(Message::getStoragePath)
                .forEach(fileStorageService::deleteIfExists);
        messageRepository.deleteAll(expiredMessages);
    }

    private long historySince() {
        return System.currentTimeMillis() - HISTORY_WINDOW.toMillis();
    }

    private ChatMessage toChatMessage(Message message) {
        return switch (message.getMessageType()) {
            case TEXT -> toTextMessage(message);
            case FILE -> toFileMessage(message);
            case SYSTEM -> toSystemMessage(message);
        };
    }

    private TextMessage toTextMessage(Message entity) {
        TextMessage message = new TextMessage(entity.getSender(), entity.getContent(), entity.getRecipient());
        message.setTimestamp(entity.getTimestamp());
        return message;
    }

    private FileMessage toFileMessage(Message entity) {
        FileMessage message = new FileMessage(
                entity.getSender(),
                entity.getRecipient(),
                entity.getFileId(),
                entity.getOriginalFilename(),
                entity.getSize() == null ? 0L : entity.getSize(),
                entity.getContentType()
        );
        message.setTimestamp(entity.getTimestamp());
        return message;
    }

    private SystemMessage toSystemMessage(Message entity) {
        SystemMessage message = new SystemMessage(entity.getContent());
        message.setTimestamp(entity.getTimestamp());
        return message;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
