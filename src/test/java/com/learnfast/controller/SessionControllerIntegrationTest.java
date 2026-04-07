package com.learnfast.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnfast.model.Role;
import com.learnfast.model.User;
import com.learnfast.repository.ChatMessageRepository;
import com.learnfast.repository.RoleRepository;
import com.learnfast.repository.SessionRepository;
import com.learnfast.repository.UserRepository;
import com.learnfast.service.AuthService;
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
 * Integration test for the session booking flow:
 * student requests a session → mentor accepts/rejects → both can list their sessions.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SessionControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private ChatMessageRepository chatMessageRepository;

    private User student;
    private User mentor;
    private MockHttpSession studentSession;
    private MockHttpSession mentorSession;

    @BeforeEach
    void setUp() throws Exception {
        sessionRepository.deleteAll();
        chatMessageRepository.deleteAll();
        userRepository.deleteAll();

        // Ensure roles exist
        if (roleRepository.findByName("student").isEmpty()) {
            roleRepository.save(new Role("student"));
        }
        if (roleRepository.findByName("mentor").isEmpty()) {
            roleRepository.save(new Role("mentor"));
        }

        // Register student via the API and capture the session
        studentSession = registerAndGetSession("student1", "student1@test.com", "pass", "student", "Student One", 20);
        student = userRepository.findByUsername("student1").orElseThrow();

        // Register mentor
        mentorSession = registerAndGetSession("mentor1", "mentor1@test.com", "pass", "mentor", "Mentor One", 30);
        mentor = userRepository.findByUsername("mentor1").orElseThrow();
    }

    @Test
    void createSession_andMentorAccepts() throws Exception {
        // 1. Student creates a session request with the mentor
        Map<String, Object> body = Map.of("mentorId", mentor.getId());

        MvcResult createResult = mockMvc.perform(post("/api/sessions")
                        .session(studentSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.roomId").exists())
                .andReturn();

        // Extract session id from response
        String responseJson = createResult.getResponse().getContentAsString();
        Long sessionId = objectMapper.readTree(responseJson).get("id").asLong();

        // 2. Mentor accepts the session
        mockMvc.perform(put("/api/sessions/" + sessionId + "/accept")
                        .session(mentorSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // 3. Student can see the accepted session in their list
        mockMvc.perform(get("/api/sessions").session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACCEPTED"));
    }

    @Test
    void createSession_andMentorRejects() throws Exception {
        // 1. Student creates a session request
        Map<String, Object> body = Map.of("mentorId", mentor.getId());

        MvcResult createResult = mockMvc.perform(post("/api/sessions")
                        .session(studentSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        Long sessionId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        // 2. Mentor rejects the session
        mockMvc.perform(put("/api/sessions/" + sessionId + "/reject")
                        .session(mentorSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void createSession_unauthenticated_returns401() throws Exception {
        Map<String, Object> body = Map.of("mentorId", 1);

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    // ---- helper ----

    private MockHttpSession registerAndGetSession(String username, String email,
                                                  String password, String role,
                                                  String name, int age) throws Exception {
        Map<String, Object> body = Map.of(
                "username", username,
                "email", email,
                "password", password,
                "role", role,
                "name", name,
                "age", age,
                "bio", ""
        );

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
