package com.chatapp.client.model;

import com.chatapp.common.model.ChatMessage;
import com.chatapp.common.model.FileMessage;
import com.chatapp.common.model.SystemMessage;
import com.chatapp.common.model.TextMessage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class MessageItem {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MMM d, HH:mm")
            .withZone(ZoneId.systemDefault());

    private final ChatMessage message;
    private final String currentUser;

    public MessageItem(ChatMessage message, String currentUser) {
        this.message = message;
        this.currentUser = currentUser;
    }

    public ChatMessage getMessage() {
        return message;
    }

    public boolean isOwnMessage() {
        String sender = getSender();
        return sender != null && sender.equals(currentUser);
    }

    public boolean isSystemMessage() {
        return message instanceof SystemMessage;
    }

    public boolean isFileMessage() {
        return message instanceof FileMessage;
    }

    public String getSender() {
        if (message instanceof TextMessage textMessage) {
            return textMessage.getSender();
        }
        if (message instanceof FileMessage fileMessage) {
            return fileMessage.getSender();
        }
        return null;
    }

    public String getRecipient() {
        if (message instanceof TextMessage textMessage) {
            return textMessage.getRecipient();
        }
        if (message instanceof FileMessage fileMessage) {
            return fileMessage.getRecipient();
        }
        return null;
    }

    public String getContent() {
        if (message instanceof TextMessage textMessage) {
            return textMessage.getContent();
        }
        if (message instanceof SystemMessage systemMessage) {
            return systemMessage.getContent();
        }
        return "";
    }

    public String getOriginalFilename() {
        if (message instanceof FileMessage fileMessage) {
            return fileMessage.getOriginalFilename();
        }
        return "";
    }

    public long getFileSize() {
        if (message instanceof FileMessage fileMessage) {
            return fileMessage.getSize();
        }
        return 0L;
    }

    public String getFileId() {
        if (message instanceof FileMessage fileMessage) {
            return fileMessage.getFileId();
        }
        return null;
    }

    public String getTimestampText() {
        return FORMATTER.format(Instant.ofEpochMilli(message.getTimestamp()));
    }

    public String getConversationKey() {
        String recipient = getRecipient();
        String sender = getSender();
        if (recipient == null || recipient.isBlank()) {
            return "GROUP";
        }
        return currentUser.equals(sender) ? recipient : sender;
    }

    public boolean belongsToGroup() {
        return "GROUP".equals(getConversationKey()) || isSystemMessage();
    }

    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        }
        double kb = size / 1024.0;
        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        return String.format("%.1f MB", mb);
    }
}
