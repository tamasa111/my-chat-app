package com.chatapp.common;

import com.chatapp.common.model.ChatMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class MessageSerializer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private MessageSerializer() {
    }

    public static String toJson(ChatMessage message) {
        try {
            return OBJECT_MAPPER.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize message", exception);
        }
    }

    public static ChatMessage fromJson(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, ChatMessage.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to deserialize message", exception);
        }
    }

    public static ObjectMapper objectMapper() {
        return OBJECT_MAPPER;
    }
}
