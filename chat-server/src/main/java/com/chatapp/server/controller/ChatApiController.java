package com.chatapp.server.controller;

import com.chatapp.common.model.ChatMessage;
import com.chatapp.common.model.FileMessage;
import com.chatapp.server.model.Message;
import com.chatapp.server.service.ChatPresenceService;
import com.chatapp.server.service.FileStorageService;
import com.chatapp.server.service.MessageService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ChatApiController {

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    private final MessageService messageService;
    private final FileStorageService fileStorageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatPresenceService chatPresenceService;

    public ChatApiController(
            MessageService messageService,
            FileStorageService fileStorageService,
            SimpMessagingTemplate messagingTemplate,
            ChatPresenceService chatPresenceService
    ) {
        this.messageService = messageService;
        this.fileStorageService = fileStorageService;
        this.messagingTemplate = messagingTemplate;
        this.chatPresenceService = chatPresenceService;
    }

    @GetMapping("/chat/history/{username}")
    public List<ChatMessage> history(@PathVariable String username) {
        return messageService.getHistoryForUser(username);
    }

    @GetMapping("/chat/group")
    public List<ChatMessage> groupHistory() {
        return messageService.getGroupHistory();
    }

    @GetMapping("/chat/p2p/{username}")
    public List<ChatMessage> peerHistory(@PathVariable String username) {
        return messageService.getPeerHistory(username);
    }

    @GetMapping("/chat/retention")
    public Map<String, Object> retention() {
        return Map.of("retentionDays", messageService.getRetentionDays());
    }

    @PostMapping(path = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileMessage upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sender") String sender,
            @RequestParam(value = "recipient", required = false) String recipient
    ) throws IOException {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Maximum file size is 50MB");
        }
        if (!StringUtils.hasText(sender)) {
            throw new IllegalArgumentException("Sender is required");
        }

        FileStorageService.StoredFile storedFile = fileStorageService.store(file);
        FileMessage message = messageService.saveFileMessage(sender.trim(), recipient, storedFile);
        if (message.getRecipient() == null || message.getRecipient().isBlank()) {
            messagingTemplate.convertAndSend("/topic/messages", message);
        } else {
            messagingTemplate.convertAndSendToUser(message.getSender(), "/queue/messages", message);
            messagingTemplate.convertAndSendToUser(message.getRecipient(), "/queue/messages", message);
        }
        return message;
    }

    @GetMapping("/files/download/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable String fileId) {
        Message message = messageService.findFile(fileId);
        Resource resource = fileStorageService.loadAsResource(message.getStoragePath());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(message.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + message.getOriginalFilename() + "\"")
                .body(resource);
    }
}
