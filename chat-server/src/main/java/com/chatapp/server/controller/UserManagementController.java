package com.chatapp.server.controller;

import com.chatapp.server.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
    public String createUser(@ModelAttribute("form") UserForm form, RedirectAttributes redirectAttributes) {
        userService.createUser(form.username(), form.password());
        redirectAttributes.addFlashAttribute("message", "User created: " + form.username());
        return "redirect:/users";
    }

    public record UserForm(String username, String password) {
        public UserForm() {
            this("", "");
        }
    }
}
