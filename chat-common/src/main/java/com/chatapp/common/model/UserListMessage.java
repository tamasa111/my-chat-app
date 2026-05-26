package com.chatapp.common.model;

import java.util.ArrayList;
import java.util.List;

public class UserListMessage extends ChatMessage {

    private List<UserStatus> users = new ArrayList<>();

    public UserListMessage() {
        super("user_list");
    }

    public UserListMessage(List<UserStatus> users) {
        this();
        this.users = users;
    }

    public List<UserStatus> getUsers() {
        return users;
    }

    public void setUsers(List<UserStatus> users) {
        this.users = users;
    }

    public static class UserStatus {
        private String username;
        private boolean online;

        public UserStatus() {
        }

        public UserStatus(String username, boolean online) {
            this.username = username;
            this.online = online;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public boolean isOnline() {
            return online;
        }

        public void setOnline(boolean online) {
            this.online = online;
        }
    }
}
