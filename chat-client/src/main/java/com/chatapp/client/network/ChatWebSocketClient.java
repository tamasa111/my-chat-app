package com.chatapp.client.network;

import com.chatapp.common.MessageSerializer;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

public class ChatWebSocketClient {

    public WebSocketStompClient createClient() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(MessageSerializer.objectMapper());
        client.setMessageConverter(converter);
        return client;
    }

    public WebSocketHttpHeaders createHeaders() {
        return new WebSocketHttpHeaders();
    }
}
