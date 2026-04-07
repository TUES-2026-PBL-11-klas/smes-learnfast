package com.learnfast.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class ChatMessageTest {

    private User sender;
    private User receiver;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setId(1L);
        sender.setName("Alice");
        sender.setUsername("alice");

        receiver = new User();
        receiver.setId(2L);
        receiver.setName("Bob");
        receiver.setUsername("bob");
    }

    @Test
    void defaultConstructor_createsInstance() {
        ChatMessage msg = new ChatMessage();
        assertThat(msg).isNotNull();
    }

    @Test
    void sentAt_defaultsToNow() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        ChatMessage msg = new ChatMessage();
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertThat(msg.getSentAt()).isBetween(before, after);
    }

    @Test
    void setAndGetId() {
        ChatMessage msg = new ChatMessage();
        msg.setId(42L);
        assertThat(msg.getId()).isEqualTo(42L);
    }

    @Test
    void setAndGetSender() {
        ChatMessage msg = new ChatMessage();
        msg.setSender(sender);
        assertThat(msg.getSender()).isEqualTo(sender);
    }

    @Test
    void setAndGetReceiver() {
        ChatMessage msg = new ChatMessage();
        msg.setReceiver(receiver);
        assertThat(msg.getReceiver()).isEqualTo(receiver);
    }

    @Test
    void setAndGetMessage() {
        ChatMessage msg = new ChatMessage();
        msg.setMessage("Hello!");
        assertThat(msg.getMessage()).isEqualTo("Hello!");
    }

    @Test
    void setAndGetSentAt() {
        ChatMessage msg = new ChatMessage();
        LocalDateTime time = LocalDateTime.of(2024, 6, 1, 12, 0);
        msg.setSentAt(time);
        assertThat(msg.getSentAt()).isEqualTo(time);
    }

    @Test
    void setMessage_allowsLongText() {
        ChatMessage msg = new ChatMessage();
        String longText = "A".repeat(5000);
        msg.setMessage(longText);
        assertThat(msg.getMessage()).hasSize(5000);
    }

    @Test
    void senderAndReceiverAreIndependent() {
        ChatMessage msg = new ChatMessage();
        msg.setSender(sender);
        msg.setReceiver(receiver);

        assertThat(msg.getSender()).isNotEqualTo(msg.getReceiver());
        assertThat(msg.getSender().getId()).isEqualTo(1L);
        assertThat(msg.getReceiver().getId()).isEqualTo(2L);
    }
}
