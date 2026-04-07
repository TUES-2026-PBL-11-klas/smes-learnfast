package com.learnfast.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnfast.dto.ChatMessageDto;
import com.learnfast.dto.UserDto;
import com.learnfast.model.ChatMessage;
import com.learnfast.model.Role;
import com.learnfast.model.User;
import com.learnfast.service.AuthService;
import com.learnfast.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ChatService chatService;
    @MockBean AuthService authService;
    @MockBean SimpMessagingTemplate messagingTemplate;

    private User sender;
    private User receiver;
    private ChatMessage savedMessage;
    private ChatMessageDto messageDto;
    private MockHttpSession authSession;
    private MockHttpSession anonSession;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setId(1L);
        sender.setUsername("alice");
        sender.setEmail("alice@example.com");
        sender.setRole(new Role("student"));
        sender.setName("Alice");
        sender.setAge(22);

        receiver = new User();
        receiver.setId(2L);
        receiver.setUsername("bob");
        receiver.setEmail("bob@example.com");
        receiver.setRole(new Role("mentor"));
        receiver.setName("Bob");
        receiver.setAge(35);

        savedMessage = new ChatMessage();
        savedMessage.setId(10L);
        savedMessage.setSender(sender);
        savedMessage.setReceiver(receiver);
        savedMessage.setMessage("Hello Bob!");
        savedMessage.setSentAt(LocalDateTime.of(2024, 6, 1, 12, 0));

        messageDto = new ChatMessageDto();
        messageDto.setId(10L);
        messageDto.setSenderId(1L);
        messageDto.setSenderName("Alice");
        messageDto.setSenderUsername("alice");
        messageDto.setReceiverId(2L);
        messageDto.setReceiverName("Bob");
        messageDto.setReceiverUsername("bob");
        messageDto.setMessage("Hello Bob!");
        messageDto.setSentAt(LocalDateTime.of(2024, 6, 1, 12, 0));

        authSession = new MockHttpSession();
        authSession.setAttribute("userId", 1L);

        anonSession = new MockHttpSession();

        when(authService.findById(1L)).thenReturn(Optional.of(sender));
    }

    // ── GET /api/chat/conversations ───────────────────────────────────────────

    @Test
    void getConversations_authenticated_returns200() throws Exception {
        when(chatService.getConversationPartners(sender)).thenReturn(List.of(receiver));

        mockMvc.perform(get("/api/chat/conversations").session(authSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getConversations_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/chat/conversations").session(anonSession))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Not authenticated"));
    }

    @Test
    void getConversations_noPartners_returnsEmptyList() throws Exception {
        when(chatService.getConversationPartners(sender)).thenReturn(List.of());

        mockMvc.perform(get("/api/chat/conversations").session(authSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── GET /api/chat/history/{otherUserId} ───────────────────────────────────

    @Test
    void getHistory_authenticated_returns200() throws Exception {
        when(chatService.getConversation(sender, 2L)).thenReturn(List.of(messageDto));

        mockMvc.perform(get("/api/chat/history/2").session(authSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].message").value("Hello Bob!"))
                .andExpect(jsonPath("$[0].senderUsername").value("alice"))
                .andExpect(jsonPath("$[0].receiverUsername").value("bob"));
    }

    @Test
    void getHistory_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/chat/history/2").session(anonSession))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Not authenticated"));
    }

    @Test
    void getHistory_emptyConversation_returnsEmptyList() throws Exception {
        when(chatService.getConversation(sender, 2L)).thenReturn(List.of());

        mockMvc.perform(get("/api/chat/history/2").session(authSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── POST /api/chat/send ───────────────────────────────────────────────────

    @Test
    void sendMessage_authenticated_returns200AndBroadcasts() throws Exception {
        when(chatService.saveMessage(eq(sender), eq(2L), eq("Hello Bob!")))
                .thenReturn(savedMessage);
        when(chatService.toDto(savedMessage)).thenReturn(messageDto);

        Map<String, Object> body = Map.of("receiverId", 2, "message", "Hello Bob!");

        mockMvc.perform(post("/api/chat/send")
                        .session(authSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.message").value("Hello Bob!"));

        // Verify WebSocket broadcasts to both sender and receiver
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/2"), eq(messageDto));
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/1"), eq(messageDto));
    }

    @Test
    void sendMessage_unauthenticated_returns401() throws Exception {
        Map<String, Object> body = Map.of("receiverId", 2, "message", "Hi!");

        mockMvc.perform(post("/api/chat/send")
                        .session(anonSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Not authenticated"));

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

}
