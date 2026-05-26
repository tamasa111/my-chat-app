package com.chatapp.common.model;

public class LoginResponse extends ChatMessage {

    private boolean success;
    private String username;
    private String message;

    public LoginResponse() {
        super("login_response");
    }

    public LoginResponse(boolean success, String username, String message) {
        this();
        this.success = success;
        this.username = username;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
