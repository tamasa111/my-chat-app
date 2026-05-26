package com.chatapp.client.network;

import com.chatapp.common.model.ChatMessage;
import com.chatapp.common.model.LoginRequest;
import com.chatapp.common.model.LoginResponse;
import com.chatapp.common.model.UserListMessage;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.messaging.WebSocketStompClient;

public class ChatStompClient {

    public interface Listener {
        void onConnected();

        void onLoginResponse(LoginResponse response);

        void onMessage(ChatMessage message);

        void onUsers(List<UserListMessage.UserStatus> users);

        void onDisconnected();

        void onError(String message, Throwable throwable);
    }

    private final String serverUrl;
    private final String username;
    private final String password;
    private final WebSocketStompClient stompClient;
    private final ExecutorService callbackExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean loginSent = new AtomicBoolean(false);
    private volatile Listener listener;
    private volatile StompSession session;

    public ChatStompClient(String serverUrl, String username, String password) {
        this.serverUrl = serverUrl;
        this.username = username;
        this.password = password;
        this.stompClient = new ChatWebSocketClient().createClient();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void connect() {
        WebSocketHttpHeaders webSocketHeaders = new ChatWebSocketClient().createHeaders();
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("username", username);

        CompletableFuture<StompSession> future = stompClient.connectAsync(serverUrl, webSocketHeaders, connectHeaders, new SessionHandler());
        future.whenComplete((stompSession, throwable) -> {
            if (throwable != null) {
                dispatchError("Unable to connect", throwable);
                return;
            }
            storeSession(stompSession);
        });
    }

    public void disconnect() {
        StompSession current = session;
        if (current != null && current.isConnected()) {
            current.disconnect();
        }
        stompClient.stop();
        Listener currentListener = listener;
        if (currentListener != null) {
            currentListener.onDisconnected();
        }
        callbackExecutor.shutdownNow();
    }

    public void sendChatMessage(ChatMessage message) {
        StompSession current = requireSession();
        current.send("/app/chat", message);
    }

    public void sendLogin() {
        if (loginSent.compareAndSet(false, true)) {
            sendChatMessage(new LoginRequest(username, password));
        }
    }

    public String getUsername() {
        return username;
    }

    public String getHttpBaseUrl() {
        if (serverUrl.startsWith("ws://")) {
            return "http://" + serverUrl.substring("ws://".length(), serverUrl.length() - 3);
        }
        if (serverUrl.startsWith("wss://")) {
            return "https://" + serverUrl.substring("wss://".length(), serverUrl.length() - 3);
        }
        if (serverUrl.endsWith("/ws")) {
            return serverUrl.substring(0, serverUrl.length() - 3);
        }
        return serverUrl;
    }

    public Map<String, String> buildUploadFields(String recipient) {
        return recipient == null || recipient.isBlank()
                ? Map.of("sender", username)
                : Map.of("sender", username, "recipient", recipient);
    }

    private void storeSession(StompSession stompSession) {
        this.session = stompSession;
    }

    private StompSession requireSession() {
        if (session == null || !session.isConnected()) {
            throw new IllegalStateException("Not connected");
        }
        return session;
    }

    private void dispatchError(String message, Throwable throwable) {
        Listener currentListener = listener;
        if (currentListener != null) {
            callbackExecutor.execute(() -> currentListener.onError(message, throwable));
        }
    }

    private void dispatch(Runnable runnable) {
        callbackExecutor.execute(runnable);
    }

    private class SessionHandler extends StompSessionHandlerAdapter {

        @Override
        public void afterConnected(StompSession stompSession, StompHeaders connectedHeaders) {
            session = stompSession;
            subscribe(stompSession, "/topic/login", LoginResponse.class, payload -> {
                if (username.equals(payload.getUsername())) {
                    listenerSafe().onLoginResponse(payload);
                }
            });
            subscribe(stompSession, "/topic/messages", ChatMessage.class, payload -> listenerSafe().onMessage(payload));
            subscribe(stompSession, "/user/queue/messages", ChatMessage.class, payload -> listenerSafe().onMessage(payload));
            subscribe(stompSession, "/topic/users", UserListMessage.class, payload -> listenerSafe().onUsers(payload.getUsers()));
            subscribe(stompSession, "/topic/user/" + username + "/history", ChatMessage.class, payload -> listenerSafe().onMessage(payload));
            dispatch(() -> listenerSafe().onConnected());
        }

        @Override
        public void handleTransportError(StompSession stompSession, Throwable exception) {
            dispatchError("Transport error", exception);
        }

        @Override
        public void handleException(StompSession stompSession, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
            dispatchError("STOMP error", exception);
        }
    }

    private <T> void subscribe(StompSession stompSession, String destination, Class<T> payloadType, java.util.function.Consumer<T> consumer) {
        stompSession.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return payloadType;
            }

            @SuppressWarnings("unchecked")
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                dispatch(() -> consumer.accept((T) payload));
            }
        });
    }

    private Listener listenerSafe() {
        Listener currentListener = listener;
        if (currentListener == null) {
            throw new IllegalStateException("Listener has not been set");
        }
        return currentListener;
    }
}
