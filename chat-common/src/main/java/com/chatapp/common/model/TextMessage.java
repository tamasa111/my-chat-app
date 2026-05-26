package com.chatapp.common.model;

public class TextMessage extends ChatMessage {

    private String sender;
    private String content;
    private String recipient;

    public TextMessage() {
        super("text");
    }

    public TextMessage(String sender, String content, String recipient) {
        this();
        this.sender = sender;
        this.content = content;
        this.recipient = recipient;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }
}
