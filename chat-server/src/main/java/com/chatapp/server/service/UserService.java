package com.chatapp.server.service;

import com.chatapp.common.model.UserListMessage;
import com.chatapp.server.model.User;
import com.chatapp.server.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    @Transactional
    public void initializeAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            createUser("admin", "admin123");
        }
    }

    @Transactional(readOnly = true)
    public boolean validateCredentials(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(User::isActive)
                .map(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElse(false);
    }

    @Transactional
    public User createUser(String username, String rawPassword) {
        String normalizedUsername = username == null ? "" : username.trim();
        if (normalizedUsername.isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setActive(true);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> listAllUsers() {
        return userRepository.findAllByOrderByUsernameAsc();
    }

    @Transactional(readOnly = true)
    public List<User> listActiveUsers() {
        return userRepository.findAllByActiveTrueOrderByUsernameAsc();
    }

    @Transactional
    public User activateUser(String username) {
        User user = findManagedUser(username);
        user.setActive(true);
        return user;
    }

    @Transactional
    public User deactivateUser(String username) {
        User user = findManagedUser(username);
        user.setActive(false);
        return user;
    }

    @Transactional(readOnly = true)
    public List<UserListMessage.UserStatus> getUserStatuses(Set<String> onlineUsers) {
        return userRepository.findAllByOrderByUsernameAsc().stream()
                .map(user -> new UserListMessage.UserStatus(user.getUsername(), onlineUsers.contains(user.getUsername()) && user.isActive()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .filter(User::isActive)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isActive(),
                true,
                true,
                true,
                AuthorityUtils.createAuthorityList("ROLE_ADMIN")
        );
    }

    private User findManagedUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }
}
