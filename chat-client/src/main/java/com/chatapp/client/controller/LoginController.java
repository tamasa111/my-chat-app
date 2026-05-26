package com.chatapp.client.controller;

import com.chatapp.client.ChatClientApplication;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField serverUrlField;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label statusLabel;

    private ChatClientApplication application;

    public void initialize(ChatClientApplication application) {
        this.application = application;
        serverUrlField.setText("ws://localhost:8080/ws");
    }

    @FXML
    private void onLogin() {
        String serverUrl = serverUrlField.getText() == null ? "" : serverUrlField.getText().trim();
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (serverUrl.isEmpty() || username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Enter server URL, username, and password.");
            return;
        }

        try {
            statusLabel.setText("");
            application.showChatView(serverUrl, username, password);
        } catch (IOException exception) {
            statusLabel.setText("Unable to load chat view.");
        }
    }
}
