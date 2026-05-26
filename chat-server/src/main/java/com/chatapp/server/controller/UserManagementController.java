package com.chatapp.server.controller;

import com.chatapp.server.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserManagementController {

    private final UserService userService;

    public UserManagementController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/users";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.listAllUsers());
        return "users";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("form", new UserForm());
        return "user-form";
    }

    @PostMapping("/users")
    public String createUser(
            @Valid @ModelAttribute("form") UserForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "user-form";
        }

        try {
            userService.createUser(form.getUsername(), form.getPassword());
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("createUser", exception.getMessage());
            return "user-form";
        }

        redirectAttributes.addFlashAttribute("message", "User created: " + form.getUsername().trim());
        return "redirect:/users";
    }

    public static class UserForm {
        @NotBlank(message = "Username is required")
        private String username = "";

        @NotBlank(message = "Password is required")
        private String password = "";

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
}
