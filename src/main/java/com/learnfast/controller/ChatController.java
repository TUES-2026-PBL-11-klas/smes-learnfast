package com.learnfast.controller;

import com.learnfast.dto.ChatMessageDto;
import com.learnfast.dto.UserDto;
import com.learnfast.model.ChatMessage;
import com.learnfast.model.MentorSession;
import com.learnfast.model.User;
import com.learnfast.repository.SessionRepository;
import com.learnfast.service.AuthService;
import com.learnfast.service.ChatService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class ChatController {

    private final ChatService chatService;
    private final AuthService authService;
    private final SimpMessagingTemplate messagingTemplate;
    private final SessionRepository sessionRepository;

    public ChatController(ChatService chatService, AuthService authService,
                          SimpMessagingTemplate messagingTemplate,
                          SessionRepository sessionRepository) {
        this.chatService = chatService;
        this.authService = authService;
        this.messagingTemplate = messagingTemplate;
        this.sessionRepository = sessionRepository;
    }

    /** Check if the current user is allowed to chat with another user. */
    @GetMapping("/api/chat/can-chat/{otherId}")
    public ResponseEntity<?> canChat(@PathVariable Long otherId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));

        User me    = authService.findById(userId).orElseThrow();
        User other = authService.findById(otherId).orElse(null);
        if (other == null) return ResponseEntity.ok(Map.of("canChat", false, "reason", "USER_NOT_FOUND"));

        String myRole    = me.getRole().getName();
        String otherRole = other.getRole().getName();

        // Chat is gated only between students and mentors.
        boolean isMentorStudentPair =
            (myRole.equals("student") && otherRole.equals("mentor")) ||
            (myRole.equals("mentor")  && otherRole.equals("student"));

        if (!isMentorStudentPair) {
            return ResponseEntity.ok(Map.of("canChat", true));
        }

        if (sessionRepository.hasAcceptedSession(userId, otherId)) {
            return ResponseEntity.ok(Map.of("canChat", true));
        }

        // Check if there's a pending session
        List<MentorSession> sessions = sessionRepository.findBetween(userId, otherId);
        boolean hasPending = sessions.stream()
            .anyMatch(s -> s.getStatus() == MentorSession.Status.PENDING);

        Map<String, Object> resp = new HashMap<>();
        resp.put("canChat", false);
        resp.put("hasPending", hasPending);
        resp.put("canRequest", myRole.equals("student"));  // only students can send requests
        resp.put("reason", hasPending ? "PENDING_SESSION" : "NO_SESSION");
        return ResponseEntity.ok(resp);
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
        String messageType = body.get("messageType") instanceof String t ? t : "TEXT";

        // Enforce session gate: student↔mentor require an ACCEPTED session.
        User receiver = authService.findById(receiverId).orElse(null);
        if (receiver != null) {
            String sRole = sender.getRole().getName();
            String rRole = receiver.getRole().getName();
            boolean gate = (sRole.equals("student") && rRole.equals("mentor")) ||
                           (sRole.equals("mentor")  && rRole.equals("student"));
            if (gate && !sessionRepository.hasAcceptedSession(userId, receiverId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Session not accepted yet"));
            }
        }

        ChatMessage saved = chatService.saveMessage(sender, receiverId, message, messageType);
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

    /**
     * Relay call signals (INVITE / ACCEPTED / DECLINED) between peers.
     * Payload must include targetUserId so we can route to the right topic.
     */
    @MessageMapping("/call.signal")
    public void handleCallSignal(@Payload Map<String, Object> payload) {
        Long targetUserId = Long.parseLong(payload.get("targetUserId").toString());
        messagingTemplate.convertAndSend("/topic/call/" + targetUserId, payload);
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload Map<String, Object> payload) {
        Long senderId = Long.parseLong(payload.get("senderId").toString());
        Long receiverId = Long.parseLong(payload.get("receiverId").toString());
        boolean typing = (boolean) payload.get("typing");

        messagingTemplate.convertAndSend("/topic/typing/" + receiverId,
            Map.of("senderId", senderId, "typing", typing));
    }
}
