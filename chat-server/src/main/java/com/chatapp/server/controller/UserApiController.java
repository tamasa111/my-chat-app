package com.chatapp.server.controller;

import com.chatapp.common.model.UserListMessage;
import com.chatapp.server.model.User;
import com.chatapp.server.service.ChatPresenceService;
import com.chatapp.server.service.UserService;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserApiController {

    private final UserService userService;
    private final ChatPresenceService chatPresenceService;

    public UserApiController(UserService userService, ChatPresenceService chatPresenceService) {
        this.userService = userService;
        this.chatPresenceService = chatPresenceService;
    }

    @GetMapping
    public List<UserListMessage.UserStatus> activeUsers() {
        return userService.getUserStatuses(chatPresenceService.getOnlineUsers()).stream()
                .filter(UserListMessage.UserStatus::isOnline)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@RequestBody CreateUserRequest request) {
        return userService.createUser(request.username(), request.password());
    }

    @PostMapping("/{username}/activate")
    public User activateUser(@PathVariable String username) {
        User user = userService.activateUser(username);
        chatPresenceService.broadcastUserList();
        return user;
    }

    @PostMapping("/{username}/deactivate")
    public User deactivateUser(@PathVariable String username) {
        User user = userService.deactivateUser(username);
        chatPresenceService.broadcastUserList();
        return user;
    }

    public record CreateUserRequest(@NotBlank String username, @NotBlank String password) {
    }
}
