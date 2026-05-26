package com.chatapp.common.model;

public class SystemMessage extends ChatMessage {

    private String content;

    public SystemMessage() {
        super("system");
    }

    public SystemMessage(String content) {
        this();
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
