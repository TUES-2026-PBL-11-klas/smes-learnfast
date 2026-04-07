package com.learnfast.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnfast.model.Role;
import com.learnfast.model.Subject;
import com.learnfast.model.User;
import com.learnfast.repository.SubjectRepository;
import com.learnfast.service.AuthService;
import com.learnfast.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean SubjectRepository subjectRepository;
    @MockBean UserService userService;
    @MockBean AuthService authService;

    private User adminUser;
    private User regularUser;
    private MockHttpSession adminSession;
    private MockHttpSession userSession;
    private MockHttpSession anonSession;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(new Role("admin"));
        adminUser.setName("Admin");
        adminUser.setAge(30);

        regularUser = new User();
        regularUser.setId(2L);
        regularUser.setUsername("alice");
        regularUser.setEmail("alice@example.com");
        regularUser.setRole(new Role("student"));
        regularUser.setName("Alice");
        regularUser.setAge(25);

        adminSession = new MockHttpSession();
        adminSession.setAttribute("userId", 1L);

        userSession = new MockHttpSession();
        userSession.setAttribute("userId", 2L);

        anonSession = new MockHttpSession();

        when(authService.findById(1L)).thenReturn(Optional.of(adminUser));
        when(authService.findById(2L)).thenReturn(Optional.of(regularUser));
    }

    // ── GET /api/admin/subjects ───────────────────────────────────────────────

    @Test
    void getSubjects_returnsListWithoutAuthCheck() throws Exception {
        Subject math = new Subject("Math");
        math.setId(1L);
        when(subjectRepository.findAll()).thenReturn(List.of(math));

        mockMvc.perform(get("/api/admin/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Math"));
    }

    // ── POST /api/admin/subjects ──────────────────────────────────────────────

    @Test
    void addSubject_asAdmin_returns200() throws Exception {
        when(subjectRepository.existsByName("Physics")).thenReturn(false);
        Subject saved = new Subject("Physics");
        saved.setId(2L);
        when(subjectRepository.save(any(Subject.class))).thenReturn(saved);

        mockMvc.perform(post("/api/admin/subjects")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Physics"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Physics"));
    }

    @Test
    void addSubject_asNonAdmin_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/subjects")
                        .session(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Physics"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Admin access required"));
    }

    @Test
    void addSubject_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/subjects")
                        .session(anonSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Physics"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void addSubject_alreadyExists_returns400() throws Exception {
        when(subjectRepository.existsByName("Math")).thenReturn(true);

        mockMvc.perform(post("/api/admin/subjects")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Math"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Subject already exists"));
    }

    // ── DELETE /api/admin/subjects/{id} ──────────────────────────────────────

    @Test
    void deleteSubject_asAdmin_returns200() throws Exception {
        doNothing().when(subjectRepository).deleteById(1L);

        mockMvc.perform(delete("/api/admin/subjects/1").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Deleted"));
    }

    @Test
    void deleteSubject_asNonAdmin_returns403() throws Exception {
        mockMvc.perform(delete("/api/admin/subjects/1").session(userSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Admin access required"));
    }

    // ── GET /api/admin/users ──────────────────────────────────────────────────

    @Test
    void getUsers_asAdmin_returns200() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(adminUser, regularUser));

        mockMvc.perform(get("/api/admin/users").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getUsers_asNonAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users").session(userSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Admin access required"));
    }

    @Test
    void getUsers_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users").session(anonSession))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /api/admin/users/{id} ──────────────────────────────────────────

    @Test
    void deleteUser_asAdmin_returns200() throws Exception {
        doNothing().when(userService).deleteUser(2L);

        mockMvc.perform(delete("/api/admin/users/2").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted"));
    }

    @Test
    void deleteUser_asNonAdmin_returns403() throws Exception {
        mockMvc.perform(delete("/api/admin/users/2").session(userSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Admin access required"));
    }
}
