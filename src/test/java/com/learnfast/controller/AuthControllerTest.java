package com.learnfast.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnfast.model.Role;
import com.learnfast.model.User;
import com.learnfast.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService authService;

    private User user;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setRole(new Role("student"));
        user.setName("Alice");
        user.setAge(25);
        user.setBio("Bio");

        session = new MockHttpSession();
    }

    // ── POST /api/auth/register ───────────────────────────────────────────────

    @Test
    void register_success_returns200AndUserDto() throws Exception {
        when(authService.register(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(user);

        Map<String, Object> body = Map.of(
                "username", "alice",
                "email", "alice@example.com",
                "password", "secret",
                "role", "student",
                "name", "Alice",
                "age", 25,
                "bio", "Bio"
        );

        mockMvc.perform(post("/api/auth/register")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("student"));
    }

    @Test
    void register_duplicateUsername_returns400() throws Exception {
        when(authService.register(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Username already exists"));

        Map<String, Object> body = Map.of(
                "username", "alice", "email", "x@x.com",
                "password", "pass", "role", "student",
                "name", "A", "age", 20, "bio", ""
        );

        mockMvc.perform(post("/api/auth/register")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already exists"));
    }

    // ── POST /api/auth/login ──────────────────────────────────────────────────

    @Test
    void login_success_returns200AndSetsSession() throws Exception {
        when(authService.login("alice", "secret")).thenReturn(user);

        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "alice", "password", "secret"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void login_invalidCredentials_returns400() throws Exception {
        when(authService.login(any(), any()))
                .thenThrow(new RuntimeException("Invalid username or password"));

        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "alice", "password", "wrong"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));
    }

    // ── POST /api/auth/logout ─────────────────────────────────────────────────

    @Test
    void logout_returns200() throws Exception {
        session.setAttribute("userId", 1L);

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out"));
    }

    // ── GET /api/auth/me ──────────────────────────────────────────────────────

    @Test
    void me_authenticated_returns200AndUserDto() throws Exception {
        session.setAttribute("userId", 1L);
        when(authService.findById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void me_notAuthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Not authenticated"));
    }

    @Test
    void me_userNotFound_returns401() throws Exception {
        session.setAttribute("userId", 99L);
        when(authService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("User not found"));
    }
}
