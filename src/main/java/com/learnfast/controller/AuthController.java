package com.learnfast.controller;

import com.learnfast.dto.UserDto;
import com.learnfast.model.User;
import com.learnfast.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> body, HttpSession session) {
        try {
            String username = (String) body.get("username");
            String email = (String) body.get("email");
            String password = (String) body.get("password");
            String role = (String) body.get("role");
            String name = (String) body.get("name");
            Integer age = body.get("age") != null ? Integer.parseInt(body.get("age").toString()) : null;
            String bio = (String) body.get("bio");

            User user = authService.register(username, email, password, role, name, age, bio);
            session.setAttribute("userId", user.getId());

            return ResponseEntity.ok(UserDto.from(user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpSession session) {
        try {
            User user = authService.login(body.get("username"), body.get("password"));
            session.setAttribute("userId", user.getId());
            return ResponseEntity.ok(UserDto.from(user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        return authService.findById(userId)
            .map(user -> ResponseEntity.ok((Object) UserDto.from(user)))
            .orElse(ResponseEntity.status(401).body(Map.of("error", "User not found")));
    }
}
