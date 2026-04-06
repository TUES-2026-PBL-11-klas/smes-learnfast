package com.learnfast;

import com.learnfast.service.ChatService;
import com.learnfast.dto.ChatMessageDto;
import com.learnfast.model.ChatMessage;
import com.learnfast.model.User;
import com.learnfast.repository.ChatMessageRepository;
import com.learnfast.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatService chatService;

    private User sender;
    private User receiver;
    private ChatMessage chatMessage;

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

        chatMessage = new ChatMessage();
        chatMessage.setId(10L);
        chatMessage.setSender(sender);
        chatMessage.setReceiver(receiver);
        chatMessage.setMessage("Hello Bob!");
        chatMessage.setSentAt(LocalDateTime.of(2024, 1, 15, 10, 0));
    }

    // ─── saveMessage ───────────────────────────────────────────────────────────

    @Test
    void saveMessage_success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        ChatMessage result = chatService.saveMessage(sender, 2L, "Hello Bob!");

        assertThat(result.getSender()).isEqualTo(sender);
        assertThat(result.getReceiver()).isEqualTo(receiver);
        assertThat(result.getMessage()).isEqualTo("Hello Bob!");
        assertThat(result.getSentAt()).isNotNull();

        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    void saveMessage_throwsWhenReceiverNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.saveMessage(sender, 99L, "Hi!"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Receiver not found");

        verify(chatMessageRepository, never()).save(any());
    }

    // ─── getConversation ───────────────────────────────────────────────────────

    @Test
    void getConversation_returnsMessages() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(chatMessageRepository.findConversation(sender, receiver))
                .thenReturn(List.of(chatMessage));

        List<ChatMessageDto> result = chatService.getConversation(sender, 2L);

        assertThat(result).hasSize(1);
        ChatMessageDto dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getSenderId()).isEqualTo(1L);
        assertThat(dto.getSenderName()).isEqualTo("Alice");
        assertThat(dto.getSenderUsername()).isEqualTo("alice");
        assertThat(dto.getReceiverId()).isEqualTo(2L);
        assertThat(dto.getReceiverName()).isEqualTo("Bob");
        assertThat(dto.getReceiverUsername()).isEqualTo("bob");
        assertThat(dto.getMessage()).isEqualTo("Hello Bob!");
    }

    @Test
    void getConversation_throwsWhenOtherUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getConversation(sender, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getConversation_returnsEmptyListWhenNoMessages() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(chatMessageRepository.findConversation(sender, receiver)).thenReturn(List.of());

        List<ChatMessageDto> result = chatService.getConversation(sender, 2L);

        assertThat(result).isEmpty();
    }

    // ─── getConversationPartners ────────────────────────────────────────────────

    @Test
    void getConversationPartners_mergesAndDeduplicates() {
        User carol = new User();
        carol.setId(3L);
        carol.setName("Carol");

        // sender appears on both sides of different conversations
        when(chatMessageRepository.findPartnersBySender(sender)).thenReturn(List.of(receiver));
        when(chatMessageRepository.findPartnersByReceiver(sender)).thenReturn(List.of(receiver, carol));

        List<User> partners = chatService.getConversationPartners(sender);

        // receiver appears twice across the two lists but must appear only once
        assertThat(partners).hasSize(2).containsExactlyInAnyOrder(receiver, carol);
    }

    @Test
    void getConversationPartners_returnsEmptyWhenNoHistory() {
        when(chatMessageRepository.findPartnersBySender(sender)).thenReturn(List.of());
        when(chatMessageRepository.findPartnersByReceiver(sender)).thenReturn(List.of());

        List<User> partners = chatService.getConversationPartners(sender);

        assertThat(partners).isEmpty();
    }

    // ─── toDto ─────────────────────────────────────────────────────────────────

    @Test
    void toDto_mapsAllFieldsCorrectly() {
        ChatMessageDto dto = chatService.toDto(chatMessage);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getSenderId()).isEqualTo(1L);
        assertThat(dto.getSenderName()).isEqualTo("Alice");
        assertThat(dto.getSenderUsername()).isEqualTo("alice");
        assertThat(dto.getReceiverId()).isEqualTo(2L);
        assertThat(dto.getReceiverName()).isEqualTo("Bob");
        assertThat(dto.getReceiverUsername()).isEqualTo("bob");
        assertThat(dto.getMessage()).isEqualTo("Hello Bob!");
        assertThat(dto.getSentAt()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 0));
    }
}
