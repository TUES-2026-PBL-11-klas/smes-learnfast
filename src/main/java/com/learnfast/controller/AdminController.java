package com.learnfast.controller;

import com.learnfast.dto.UserDto;
import com.learnfast.exception.ResourceNotFoundException;
import com.learnfast.model.Subject;
import com.learnfast.model.User;
import com.learnfast.repository.SubjectRepository;
import com.learnfast.repository.UserRepository;
import com.learnfast.service.AuthService;
import com.learnfast.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SubjectRepository subjectRepository;
    private final UserService userService;
    private final AuthService authService;
    private final UserRepository userRepository;

    public AdminController(SubjectRepository subjectRepository, UserService userService,
                           AuthService authService, UserRepository userRepository) {
        this.subjectRepository = subjectRepository;
        this.userService = userService;
        this.authService = authService;
        this.userRepository = userRepository;
    }

    private boolean isAdmin(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return false;
        return authService.findById(userId)
            .map(u -> "admin".equals(u.getRole().getName()))
            .orElse(false);
    }

    // === Subjects ===
    @GetMapping("/subjects")
    public ResponseEntity<?> getSubjects() {
        return ResponseEntity.ok(subjectRepository.findAll());
    }

    @PostMapping("/subjects")
    public ResponseEntity<?> addSubject(@RequestBody Map<String, String> body, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        String name = body.get("name");
        if (subjectRepository.existsByName(name)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Subject already exists"));
        }
        Subject subject = subjectRepository.save(new Subject(name));
        return ResponseEntity.ok(subject);
    }

    @DeleteMapping("/subjects/{id}")
    public ResponseEntity<?> deleteSubject(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        subjectRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    // === Users ===
    @GetMapping("/users")
    public ResponseEntity<?> getUsers(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        List<UserDto> users = userService.getAllUsers().stream()
            .map(UserDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "User deleted"));
    }

    // === Mentor Approval ===

    @GetMapping("/mentors/pending")
    public ResponseEntity<?> getPendingMentors(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        List<UserDto> pending = userRepository.findByRoleNameAndStatus("mentor", "PENDING_APPROVAL")
            .stream().map(UserDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(pending);
    }

    @PutMapping("/mentors/{id}/approve")
    public ResponseEntity<?> approveMentor(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        User mentor = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        mentor.setStatus("ACTIVE");
        userRepository.save(mentor);
        return ResponseEntity.ok(Map.of("message", "Mentor approved"));
    }

    @PutMapping("/mentors/{id}/reject")
    public ResponseEntity<?> rejectMentor(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        User mentor = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        mentor.setStatus("REJECTED");
        userRepository.save(mentor);
        return ResponseEntity.ok(Map.of("message", "Mentor rejected"));
    }
}
