package com.learnfast.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class ChatMessageDtoTest {

    @Test
    void defaultConstructor_createsInstance() {
        ChatMessageDto dto = new ChatMessageDto();
        assertThat(dto).isNotNull();
    }

    @Test
    void allFieldsNullByDefault() {
        ChatMessageDto dto = new ChatMessageDto();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getSenderId()).isNull();
        assertThat(dto.getSenderName()).isNull();
        assertThat(dto.getSenderUsername()).isNull();
        assertThat(dto.getReceiverId()).isNull();
        assertThat(dto.getReceiverName()).isNull();
        assertThat(dto.getReceiverUsername()).isNull();
        assertThat(dto.getMessage()).isNull();
        assertThat(dto.getSentAt()).isNull();
    }

    @Test
    void setAndGetId() {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setId(1L);
        assertThat(dto.getId()).isEqualTo(1L);
    }

    @Test
    void setAndGetSenderId() {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setSenderId(10L);
        assertThat(dto.getSenderId()).isEqualTo(10L);
    }

    @Test
    void setAndGetSenderName() {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setSenderName("Alice");
        assertThat(dto.getSenderName()).isEqualTo("Alice");
    }

    @Test
    void setAndGetSenderUsername() {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setSenderUsername("alice123");
        assertThat(dto.getSenderUsername()).isEqualTo("alice123");
    }

    @Test
    void setAndGetReceiverId() {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setReceiverId(20L);
        assertThat(dto.getReceiverId()).isEqualTo(20L);
    }

    @Test
    void setAndGetReceiverName() {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setReceiverName("Bob");
        assertThat(dto.getReceiverName()).isEqualTo("Bob");
    }

    @Test
    void setAndGetReceiverUsername() {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setReceiverUsername("bob456");
        assertThat(dto.getReceiverUsername()).isEqualTo("bob456");
    }

    @Test
    void setAndGetMessage() {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setMessage("Hello!");
        assertThat(dto.getMessage()).isEqualTo("Hello!");
    }

    @Test
    void setAndGetSentAt() {
        ChatMessageDto dto = new ChatMessageDto();
        LocalDateTime time = LocalDateTime.of(2024, 6, 1, 12, 0);
        dto.setSentAt(time);
        assertThat(dto.getSentAt()).isEqualTo(time);
    }

    @Test
    void fullyPopulatedDto_hasAllFieldsSet() {
        ChatMessageDto dto = new ChatMessageDto();
        LocalDateTime time = LocalDateTime.of(2024, 6, 1, 12, 0);

        dto.setId(1L);
        dto.setSenderId(10L);
        dto.setSenderName("Alice");
        dto.setSenderUsername("alice");
        dto.setReceiverId(20L);
        dto.setReceiverName("Bob");
        dto.setReceiverUsername("bob");
        dto.setMessage("Hey Bob!");
        dto.setSentAt(time);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getSenderId()).isEqualTo(10L);
        assertThat(dto.getSenderName()).isEqualTo("Alice");
        assertThat(dto.getSenderUsername()).isEqualTo("alice");
        assertThat(dto.getReceiverId()).isEqualTo(20L);
        assertThat(dto.getReceiverName()).isEqualTo("Bob");
        assertThat(dto.getReceiverUsername()).isEqualTo("bob");
        assertThat(dto.getMessage()).isEqualTo("Hey Bob!");
        assertThat(dto.getSentAt()).isEqualTo(time);
    }

    @Test
    void senderAndReceiverIdsAreIndependent() {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setSenderId(1L);
        dto.setReceiverId(2L);
        assertThat(dto.getSenderId()).isNotEqualTo(dto.getReceiverId());
    }
}
