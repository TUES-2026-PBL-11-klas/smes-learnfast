package com.learnfast.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnfast.model.Role;
import com.learnfast.repository.ChatMessageRepository;
import com.learnfast.repository.RoleRepository;
import com.learnfast.repository.SessionRepository;
import com.learnfast.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for the full authentication flow:
 * register → login → get current user (/me) → logout → verify logged out.
 *
 * Uses an in-memory H2 database so no external dependencies are needed.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private ChatMessageRepository chatMessagesRepository;
    @BeforeEach
    void setUp() {
        sessionRepository.deleteAll();
        chatMessagesRepository.deleteAll();
        userRepository.deleteAll();
        // Ensure the "student" role exists
        if (roleRepository.findByName("student").isEmpty()) {
            roleRepository.save(new Role("student"));
        }
    }

    @Test
    void fullAuthFlow_registerLoginMeLogout() throws Exception {
        // 1. Register a new student
        Map<String, Object> registerBody = Map.of(
                "username", "alice",
                "email", "alice@example.com",
                "password", "secret123",
                "role", "student",
                "name", "Alice Smith",
                "age", 22,
                "bio", "Eager learner"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andReturn();

        // Capture session so subsequent requests are authenticated
        MockHttpSession session = (MockHttpSession) registerResult.getRequest().getSession(false);

        // 2. GET /me should return the registered user
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));

        // 3. Logout
        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk());

        // 4. After logout, /me should return 401
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        // 5. Login again with the same credentials
        Map<String, String> loginBody = Map.of(
                "username", "alice",
                "password", "secret123"
        );

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andReturn();

        MockHttpSession loginSession = (MockHttpSession) loginResult.getRequest().getSession(false);

        // 6. /me works with the new session
        mockMvc.perform(get("/api/auth/me").session(loginSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void register_duplicateUsername_returnsBadRequest() throws Exception {
        Map<String, Object> body = Map.of(
                "username", "bob",
                "email", "bob@example.com",
                "password", "pass",
                "role", "student",
                "name", "Bob",
                "age", 20,
                "bio", ""
        );

        // First registration succeeds
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        // Second registration with same username fails
        Map<String, Object> duplicate = Map.of(
                "username", "bob",
                "email", "bob2@example.com",
                "password", "pass",
                "role", "student",
                "name", "Bob2",
                "age", 21,
                "bio", ""
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already exists"));
    }
}
