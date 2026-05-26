package com.chatapp.common.model;

public class LoginRequest extends ChatMessage {

    private String username;
    private String password;

    public LoginRequest() {
        super("login");
    }

    public LoginRequest(String username, String password) {
        this();
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
