package com.chatapp.common.model;

public class FileMessage extends ChatMessage {

    private String sender;
    private String recipient;
    private String fileId;
    private String originalFilename;
    private long size;
    private String contentType;

    public FileMessage() {
        super("file");
    }

    public FileMessage(String sender, String recipient, String fileId, String originalFilename, long size, String contentType) {
        this();
        this.sender = sender;
        this.recipient = recipient;
        this.fileId = fileId;
        this.originalFilename = originalFilename;
        this.size = size;
        this.contentType = contentType;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
