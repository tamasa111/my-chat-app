package com.chatapp.client.controller;

import com.chatapp.client.ChatClientApplication;
import com.chatapp.client.model.MessageItem;
import com.chatapp.client.network.ChatStompClient;
import com.chatapp.common.model.ChatMessage;
import com.chatapp.common.model.FileMessage;
import com.chatapp.common.model.LoginResponse;
import com.chatapp.common.model.SystemMessage;
import com.chatapp.common.model.TextMessage;
import com.chatapp.common.model.UserListMessage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ChatController implements ChatStompClient.Listener {

    private static final String GROUP_KEY = "GROUP";

    @FXML
    private Label chatTitleLabel;

    @FXML
    private Button backButton;

    @FXML
    private ListView<MessageItem> messageListView;

    @FXML
    private ListView<UserEntry> userListView;

    @FXML
    private TextField messageInputField;

    @FXML
    private Label statusLabel;

    @FXML
    private ProgressBar uploadProgressBar;

    @FXML
    private Label uploadStatusLabel;

    private final ObservableList<MessageItem> allMessages = FXCollections.observableArrayList();
    private final ObservableList<MessageItem> visibleMessages = FXCollections.observableArrayList();
    private final ObservableList<UserEntry> users = FXCollections.observableArrayList();
    private final Map<String, Integer> unreadCounts = new HashMap<>();
    private final Map<String, Boolean> onlineUsers = new HashMap<>();
    private final SimpleStringProperty activeConversation = new SimpleStringProperty(GROUP_KEY);

    private ChatClientApplication application;
    private ChatStompClient chatStompClient;
    private String currentUsername;
    private boolean loginConfirmed;

    public void initialize(ChatClientApplication application, ChatStompClient chatStompClient, String currentUsername) {
        this.application = application;
        this.chatStompClient = chatStompClient;
        this.currentUsername = currentUsername;
        this.chatStompClient.setListener(this);

        chatTitleLabel.setText("Group Chat");
        backButton.setVisible(false);
        uploadProgressBar.setProgress(0);
        uploadProgressBar.setVisible(false);
        uploadStatusLabel.setText("");
        statusLabel.setText("Connecting...");

        messageListView.setItems(visibleMessages);
        messageListView.setCellFactory(listView -> new MessageCell());
        userListView.setItems(users);
        userListView.setCellFactory(listView -> new UserCell());
        userListView.setOnMouseClicked(event -> {
            UserEntry selected = userListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                switchConversation(selected.username());
            }
        });

        activeConversation.addListener((observable, oldValue, newValue) -> refreshVisibleMessages());
    }

    @FXML
    private void onSendMessage() {
        if (!loginConfirmed) {
            statusLabel.setText("Wait for login confirmation.");
            return;
        }

        String content = messageInputField.getText() == null ? "" : messageInputField.getText().trim();
        if (content.isEmpty()) {
            return;
        }

        String recipient = GROUP_KEY.equals(activeConversation.get()) ? null : activeConversation.get();
        chatStompClient.sendChatMessage(new TextMessage(currentUsername, content, recipient));
        messageInputField.clear();
    }

    @FXML
    private void onBackToGroup() {
        switchConversation(GROUP_KEY);
        userListView.getSelectionModel().clearSelection();
    }

    @FXML
    private void onDisconnect() {
        try {
            application.disconnectClient();
            application.showLoginView();
        } catch (IOException exception) {
            statusLabel.setText("Unable to return to login.");
        }
    }

    @FXML
    private void onAttachFile() {
        if (!loginConfirmed) {
            statusLabel.setText("Log in before sharing files.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        Path selectedPath;
        if (messageListView.getScene() != null && messageListView.getScene().getWindow() instanceof Stage stage) {
            java.io.File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile == null) {
                return;
            }
            selectedPath = selectedFile.toPath();
        } else {
            return;
        }

        String recipient = GROUP_KEY.equals(activeConversation.get()) ? null : activeConversation.get();
        startUpload(selectedPath, recipient);
    }

    @Override
    public void onConnected() {
        Platform.runLater(() -> {
            statusLabel.setText("Connected. Logging in...");
            chatStompClient.sendLogin();
        });
    }

    @Override
    public void onLoginResponse(LoginResponse response) {
        Platform.runLater(() -> {
            if (!response.isSuccess()) {
                statusLabel.setText(response.getMessage());
                return;
            }
            loginConfirmed = true;
            statusLabel.setText("Connected as " + response.getUsername());
        });
    }

    @Override
    public void onMessage(ChatMessage message) {
        Platform.runLater(() -> handleIncomingMessage(message));
    }

    @Override
    public void onUsers(List<UserListMessage.UserStatus> userStatuses) {
        Platform.runLater(() -> {
            onlineUsers.clear();
            List<UserEntry> entries = new ArrayList<>();
            for (UserListMessage.UserStatus status : userStatuses) {
                if (currentUsername.equals(status.getUsername())) {
                    continue;
                }
                onlineUsers.put(status.getUsername(), status.isOnline());
                entries.add(new UserEntry(status.getUsername(), status.isOnline(), unreadCounts.getOrDefault(status.getUsername(), 0)));
            }
            entries.sort(Comparator.comparing(UserEntry::username, String.CASE_INSENSITIVE_ORDER));
            users.setAll(entries);
        });
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            loginConfirmed = false;
            statusLabel.setText("Disconnected.");
        });
    }

    @Override
    public void onError(String message, Throwable throwable) {
        Platform.runLater(() -> statusLabel.setText(throwable == null ? message : message + ": " + throwable.getMessage()));
    }

    public void downloadFile(String fileId, String originalFilename) {
        if (fileId == null || fileId.isBlank()) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(originalFilename);

        if (!(messageListView.getScene().getWindow() instanceof Stage stage)) {
            return;
        }

        java.io.File targetFile = chooser.showSaveDialog(stage);
        if (targetFile == null) {
            return;
        }

        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Downloading " + originalFilename);
                URI uri = URI.create(chatStompClient.getHttpBaseUrl() + "/api/files/download/" + fileId);
                HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
                connection.setRequestMethod("GET");
                int length = connection.getContentLength();

                try (InputStream input = new BufferedInputStream(connection.getInputStream());
                     OutputStream output = new BufferedOutputStream(Files.newOutputStream(targetFile.toPath()))) {
                    byte[] buffer = new byte[8192];
                    long total = 0;
                    int read;
                    while ((read = input.read(buffer)) >= 0) {
                        output.write(buffer, 0, read);
                        total += read;
                        if (length > 0) {
                            updateProgress(total, length);
                        }
                    }
                }
                updateMessage("Download complete: " + originalFilename);
                return null;
            }
        };

        bindTaskProgress(downloadTask);
        new Thread(downloadTask, "file-download").start();
    }

    private void handleIncomingMessage(ChatMessage message) {
        MessageItem item = new MessageItem(message, currentUsername);
        allMessages.add(item);
        allMessages.sort(Comparator.comparingLong(value -> value.getMessage().getTimestamp()));

        if (!isVisibleInCurrentConversation(item)) {
            String key = item.getConversationKey();
            if (!GROUP_KEY.equals(key) && !currentUsername.equals(item.getSender())) {
                unreadCounts.merge(key, 1, Integer::sum);
                refreshUserEntries();
            }
        }

        refreshVisibleMessages();
    }

    private void refreshVisibleMessages() {
        visibleMessages.setAll(allMessages.filtered(this::isVisibleInCurrentConversation));
        if (!visibleMessages.isEmpty()) {
            messageListView.scrollTo(visibleMessages.size() - 1);
        }
        backButton.setVisible(!GROUP_KEY.equals(activeConversation.get()));
        chatTitleLabel.setText(GROUP_KEY.equals(activeConversation.get())
                ? "Group Chat"
                : "Chat with: " + activeConversation.get());
    }

    private boolean isVisibleInCurrentConversation(MessageItem item) {
        if (item.isSystemMessage()) {
            return GROUP_KEY.equals(activeConversation.get());
        }
        if (GROUP_KEY.equals(activeConversation.get())) {
            return item.belongsToGroup();
        }
        return activeConversation.get().equals(item.getConversationKey());
    }

    private void switchConversation(String conversationKey) {
        activeConversation.set(conversationKey);
        unreadCounts.remove(conversationKey);
        refreshUserEntries();
    }

    private void refreshUserEntries() {
        List<UserEntry> refreshed = users.stream()
                .map(entry -> new UserEntry(entry.username(), onlineUsers.getOrDefault(entry.username(), false), unreadCounts.getOrDefault(entry.username(), 0)))
                .sorted(Comparator.comparing(UserEntry::username, String.CASE_INSENSITIVE_ORDER))
                .toList();
        users.setAll(refreshed);
    }

    private void startUpload(Path file, String recipient) {
        Task<Void> uploadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String boundary = "----ChatBoundary" + System.currentTimeMillis();
                URL url = URI.create(chatStompClient.getHttpBaseUrl() + "/api/files/upload").toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                byte[] fileBytes = Files.readAllBytes(file);
                String recipientPart = recipient == null || recipient.isBlank()
                        ? ""
                        : buildFormField("recipient", recipient, boundary);
                String senderPart = buildFormField("sender", currentUsername, boundary);
                String fileHeader = "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getFileName() + "\"\r\n"
                        + "Content-Type: application/octet-stream\r\n\r\n";
                String end = "\r\n--" + boundary + "--\r\n";

                byte[] prefix = (senderPart + recipientPart + fileHeader).getBytes(StandardCharsets.UTF_8);
                byte[] suffix = end.getBytes(StandardCharsets.UTF_8);
                long totalBytes = prefix.length + fileBytes.length + suffix.length;

                updateProgress(0, totalBytes);
                updateMessage("Uploading " + file.getFileName());

                try (OutputStream outputStream = new BufferedOutputStream(connection.getOutputStream())) {
                    outputStream.write(prefix);
                    long written = prefix.length;
                    updateProgress(written, totalBytes);

                    int chunkSize = 8192;
                    for (int start = 0; start < fileBytes.length; start += chunkSize) {
                        int length = Math.min(chunkSize, fileBytes.length - start);
                        outputStream.write(fileBytes, start, length);
                        written += length;
                        updateProgress(written, totalBytes);
                    }

                    outputStream.write(suffix);
                    written += suffix.length;
                    updateProgress(written, totalBytes);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    throw new IOException("Upload failed with status " + responseCode);
                }

                updateMessage("Upload complete: " + file.getFileName());
                return null;
            }
        };

        bindTaskProgress(uploadTask);
        new Thread(uploadTask, "file-upload").start();
    }

    private void bindTaskProgress(Task<Void> task) {
        uploadProgressBar.progressProperty().unbind();
        uploadStatusLabel.textProperty().unbind();
        uploadProgressBar.progressProperty().bind(task.progressProperty());
        uploadStatusLabel.textProperty().bind(task.messageProperty());
        uploadProgressBar.setVisible(true);
        task.setOnSucceeded(event -> {
            uploadProgressBar.progressProperty().unbind();
            uploadStatusLabel.textProperty().unbind();
            uploadProgressBar.setProgress(0);
            uploadProgressBar.setVisible(false);
            uploadStatusLabel.setText(task.getMessage());
        });
        task.setOnFailed(event -> {
            uploadProgressBar.progressProperty().unbind();
            uploadStatusLabel.textProperty().unbind();
            uploadProgressBar.setProgress(0);
            uploadProgressBar.setVisible(false);
            Throwable throwable = task.getException();
            uploadStatusLabel.setText(throwable == null ? "Task failed" : throwable.getMessage());
        });
    }

    private String buildFormField(String name, String value, String boundary) {
        return "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + value + "\r\n";
    }

    private class UserCell extends ListCell<UserEntry> {
        @Override
        protected void updateItem(UserEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            Circle dot = new Circle(5, item.online() ? Color.web("#38d996") : Color.web("#6d7486"));
            Label name = new Label(item.username());
            name.getStyleClass().add("sidebar-user");
            Label unread = new Label(item.unreadCount() > 0 ? "(" + item.unreadCount() + ")" : "");
            unread.getStyleClass().add("unread-count");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox row = new HBox(10, dot, name, spacer, unread);
            row.setAlignment(Pos.CENTER_LEFT);
            setGraphic(row);
        }
    }

    private class MessageCell extends ListCell<MessageItem> {
        @Override
        protected void updateItem(MessageItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            if (item.isSystemMessage()) {
                Label label = new Label(item.getContent());
                label.getStyleClass().add("system-message");
                BorderPane container = new BorderPane(label);
                BorderPane.setAlignment(label, Pos.CENTER);
                setGraphic(container);
                return;
            }

            VBox bubble = new VBox(8);
            bubble.getStyleClass().add(item.isOwnMessage() ? "bubble-own" : "bubble-other");
            bubble.setPadding(new Insets(12));
            bubble.setMaxWidth(420);

            Label sender = new Label(item.getSender());
            sender.getStyleClass().add("message-sender");
            bubble.getChildren().add(sender);

            if (item.isFileMessage()) {
                Label fileName = new Label(item.getOriginalFilename());
                fileName.getStyleClass().add("file-name");
                Label metadata = new Label(MessageItem.formatFileSize(item.getFileSize()) + " • " + item.getTimestampText());
                metadata.getStyleClass().add("message-meta");
                Button download = new Button("Download");
                download.getStyleClass().add("download-button");
                download.setOnAction(event -> downloadFile(item.getFileId(), item.getOriginalFilename()));
                bubble.getChildren().addAll(fileName, metadata, download);
            } else {
                Label content = new Label(item.getContent());
                content.getStyleClass().add("message-content");
                content.setWrapText(true);
                Label timestamp = new Label(item.getTimestampText());
                timestamp.getStyleClass().add("message-meta");
                bubble.getChildren().addAll(content, timestamp);
            }

            BorderPane wrapper = new BorderPane();
            if (item.isOwnMessage()) {
                wrapper.setRight(bubble);
            } else {
                wrapper.setLeft(bubble);
            }
            setGraphic(wrapper);
        }
    }

    public record UserEntry(String username, boolean online, int unreadCount) {
    }
}
