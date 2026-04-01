package com.learnfast.controller;

import com.learnfast.dto.SessionDto;
import com.learnfast.model.MentorSession;
import com.learnfast.model.User;
import com.learnfast.service.AuthService;
import com.learnfast.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;
    private final AuthService authService;

    public SessionController(SessionService sessionService, AuthService authService) {
        this.sessionService = sessionService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<?> createSession(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        User student = authService.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Long mentorId = Long.parseLong(body.get("mentorId").toString());
        MentorSession mentorSession = sessionService.createSession(student, mentorId);

        return ResponseEntity.ok(sessionService.toDto(mentorSession));
    }

    @GetMapping
    public ResponseEntity<?> getSessions(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        User user = authService.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        List<SessionDto> sessions = sessionService.getUserSessions(user);
        return ResponseEntity.ok(sessions);
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<?> acceptSession(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        User mentor = authService.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        MentorSession mentorSession = sessionService.acceptSession(id, mentor);
        return ResponseEntity.ok(sessionService.toDto(mentorSession));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectSession(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        User mentor = authService.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        MentorSession mentorSession = sessionService.rejectSession(id, mentor);
        return ResponseEntity.ok(sessionService.toDto(mentorSession));
    }
}
