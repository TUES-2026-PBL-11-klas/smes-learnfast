package com.learnfast.controller;

import com.learnfast.dto.ChatMessageDto;
import com.learnfast.dto.UserDto;
import com.learnfast.model.ChatMessage;
import com.learnfast.model.User;
import com.learnfast.service.AuthService;
import com.learnfast.service.ChatService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class ChatController {

    private final ChatService chatService;
    private final AuthService authService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService, AuthService authService,
                          SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.authService = authService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/api/chat/conversations")
    public ResponseEntity<?> getConversations(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        User user = authService.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        List<UserDto> partners = chatService.getConversationPartners(user)
            .stream().map(UserDto::from).collect(Collectors.toList());

        return ResponseEntity.ok(partners);
    }

    @GetMapping("/api/chat/history/{otherUserId}")
    public ResponseEntity<?> getHistory(@PathVariable Long otherUserId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        User user = authService.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        List<ChatMessageDto> messages = chatService.getConversation(user, otherUserId);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/api/chat/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        User sender = authService.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Long receiverId = Long.parseLong(body.get("receiverId").toString());
        String message = (String) body.get("message");

        ChatMessage saved = chatService.saveMessage(sender, receiverId, message);
        ChatMessageDto dto = chatService.toDto(saved);

        // Send via WebSocket to the receiver
        messagingTemplate.convertAndSend("/topic/chat/" + receiverId, dto);
        messagingTemplate.convertAndSend("/topic/chat/" + userId, dto);

        return ResponseEntity.ok(dto);
    }

    @MessageMapping("/chat.send")
    public void handleWebSocketMessage(@Payload Map<String, Object> payload) {
        Long senderId = Long.parseLong(payload.get("senderId").toString());
        Long receiverId = Long.parseLong(payload.get("receiverId").toString());
        String message = (String) payload.get("message");

        User sender = authService.findById(senderId)
            .orElseThrow(() -> new RuntimeException("Sender not found"));

        ChatMessage saved = chatService.saveMessage(sender, receiverId, message);
        ChatMessageDto dto = chatService.toDto(saved);

        messagingTemplate.convertAndSend("/topic/chat/" + receiverId, dto);
        messagingTemplate.convertAndSend("/topic/chat/" + senderId, dto);
    }
}
