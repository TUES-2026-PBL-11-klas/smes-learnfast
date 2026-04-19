package com.learnfast.controller;

import com.learnfast.dto.UserDto;
import com.learnfast.model.User;
import com.learnfast.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** All non-admin users — used by mentors when adding channel members. */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        List<UserDto> users = userService.getAllUsers().stream()
            .filter(u -> !"admin".equals(u.getRole().getName()))
            .map(UserDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/mentors")
    public ResponseEntity<List<UserDto>> getMentors() {
        List<UserDto> mentors = userService.getMentors().stream()
            .map(UserDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(mentors);
    }

    @GetMapping("/mentors/{id}")
    public ResponseEntity<?> getMentor(@PathVariable Long id) {
        return userService.findById(id)
            .map(user -> ResponseEntity.ok((Object) UserDto.from(user)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        User user = userService.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        String name = (String) body.get("name");
        String bio = (String) body.get("bio");
        Integer age = body.get("age") != null ? Integer.parseInt(body.get("age").toString()) : null;
        String avatarUrl = (String) body.get("avatarUrl");

        user = userService.updateProfile(user, name, bio, age, avatarUrl);
        return ResponseEntity.ok(UserDto.from(user));
    }

    @PutMapping("/profile/subjects")
    public ResponseEntity<?> updateSubjects(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        User user = userService.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        @SuppressWarnings("unchecked")
        List<Number> subjectIdsList = (List<Number>) body.get("subjectIds");
        Set<Long> subjectIds = subjectIdsList.stream()
            .map(Number::longValue).collect(Collectors.toSet());

        user = userService.updateSubjects(user, subjectIds);
        return ResponseEntity.ok(UserDto.from(user));
    }
}
