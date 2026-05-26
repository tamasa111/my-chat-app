package com.chatapp.client;

import com.chatapp.client.controller.ChatController;
import com.chatapp.client.controller.LoginController;
import com.chatapp.client.network.ChatStompClient;
import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ChatClientApplication extends Application {

    private Stage primaryStage;
    private Scene scene;
    private ChatStompClient chatStompClient;

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        this.primaryStage.setTitle("Chat Application");
        showLoginView();
        this.primaryStage.show();
    }

    public void showLoginView() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Parent root = loader.load();
        LoginController controller = loader.getController();
        controller.initialize(this);

        if (scene == null) {
            scene = new Scene(root, 400, 300);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            primaryStage.setScene(scene);
        } else {
            scene.setRoot(root);
        }

        primaryStage.setWidth(400);
        primaryStage.setHeight(300);
        primaryStage.centerOnScreen();
    }

    public void showChatView(String serverUrl, String username, String password) throws IOException {
        if (chatStompClient != null) {
            chatStompClient.disconnect();
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
        Parent root = loader.load();
        ChatController controller = loader.getController();

        chatStompClient = new ChatStompClient(serverUrl, username, password);
        controller.initialize(this, chatStompClient, username);

        scene.setRoot(root);
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(400);
        primaryStage.setWidth(1000);
        primaryStage.setHeight(700);
        primaryStage.centerOnScreen();

        chatStompClient.connect();
    }

    public void disconnectClient() {
        if (chatStompClient != null) {
            chatStompClient.disconnect();
            chatStompClient = null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
