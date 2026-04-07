package com.learnfast.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnfast.dto.UserDto;
import com.learnfast.model.Role;
import com.learnfast.model.Subject;
import com.learnfast.model.User;
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
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserService userService;

    private User mentor;
    private User student;
    private MockHttpSession authSession;
    private MockHttpSession anonSession;

    @BeforeEach
    void setUp() {
        mentor = new User();
        mentor.setId(1L);
        mentor.setUsername("bob");
        mentor.setEmail("bob@example.com");
        mentor.setRole(new Role("mentor"));
        mentor.setName("Bob");
        mentor.setAge(35);
        mentor.setBio("Experienced mentor");

        student = new User();
        student.setId(2L);
        student.setUsername("alice");
        student.setEmail("alice@example.com");
        student.setRole(new Role("student"));
        student.setName("Alice");
        student.setAge(22);
        student.setBio("Learning Java");

        authSession = new MockHttpSession();
        authSession.setAttribute("userId", 2L);

        anonSession = new MockHttpSession();

        when(userService.findById(2L)).thenReturn(Optional.of(student));
    }

    // ── GET /api/mentors ──────────────────────────────────────────────────────

    @Test
    void getMentors_returnsListOfMentorDtos() throws Exception {
        when(userService.getMentors()).thenReturn(List.of(mentor));

        mockMvc.perform(get("/api/mentors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("bob"))
                .andExpect(jsonPath("$[0].role").value("mentor"));
    }

    @Test
    void getMentors_noAuthRequired_returnsOk() throws Exception {
        when(userService.getMentors()).thenReturn(List.of());

        mockMvc.perform(get("/api/mentors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── GET /api/mentors/{id} ─────────────────────────────────────────────────

    @Test
    void getMentor_found_returns200() throws Exception {
        when(userService.findById(1L)).thenReturn(Optional.of(mentor));

        mockMvc.perform(get("/api/mentors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bob"))
                .andExpect(jsonPath("$.name").value("Bob"));
    }

    @Test
    void getMentor_notFound_returns404() throws Exception {
        when(userService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/mentors/99"))
                .andExpect(status().isNotFound());
    }

    // ── PUT /api/profile ──────────────────────────────────────────────────────

    @Test
    void updateProfile_authenticated_returns200() throws Exception {
        User updated = new User();
        updated.setId(2L);
        updated.setUsername("alice");
        updated.setEmail("alice@example.com");
        updated.setRole(new Role("student"));
        updated.setName("Alice Updated");
        updated.setAge(23);
        updated.setBio("Updated bio");

        when(userService.updateProfile(eq(student), eq("Alice Updated"), eq("Updated bio"),
                eq(23), isNull())).thenReturn(updated);

        Map<String, Object> body = Map.of("name", "Alice Updated", "bio", "Updated bio", "age", 23);

        mockMvc.perform(put("/api/profile")
                        .session(authSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Updated"))
                .andExpect(jsonPath("$.bio").value("Updated bio"));
    }

    @Test
    void updateProfile_unauthenticated_returns401() throws Exception {
        Map<String, Object> body = Map.of("name", "Alice");

        mockMvc.perform(put("/api/profile")
                        .session(anonSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Not authenticated"));
    }

    @Test
    void updateProfile_withAvatarUrl_passedThrough() throws Exception {
        User updated = new User();
        updated.setId(2L);
        updated.setUsername("alice");
        updated.setEmail("alice@example.com");
        updated.setRole(new Role("student"));
        updated.setName("Alice");
        updated.setAge(22);
        updated.setAvatarUrl("https://example.com/new.png");

        when(userService.updateProfile(eq(student), any(), any(), any(),
                eq("https://example.com/new.png"))).thenReturn(updated);

        Map<String, Object> body = Map.of("avatarUrl", "https://example.com/new.png");

        mockMvc.perform(put("/api/profile")
                        .session(authSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/new.png"));
    }

    // ── PUT /api/profile/subjects ─────────────────────────────────────────────

    @Test
    void updateSubjects_authenticated_returns200() throws Exception {
        Subject math = new Subject("Math");
        math.setId(10L);
        Subject science = new Subject("Science");
        science.setId(11L);

        User updated = new User();
        updated.setId(2L);
        updated.setUsername("alice");
        updated.setEmail("alice@example.com");
        updated.setRole(new Role("student"));
        updated.setName("Alice");
        updated.setAge(22);
        updated.setSubjects(Set.of(math, science));

        when(userService.updateSubjects(eq(student), eq(Set.of(10L, 11L)))).thenReturn(updated);

        Map<String, Object> body = Map.of("subjectIds", List.of(10, 11));

        mockMvc.perform(put("/api/profile/subjects")
                        .session(authSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects.length()").value(2));
    }

    @Test
    void updateSubjects_unauthenticated_returns401() throws Exception {
        Map<String, Object> body = Map.of("subjectIds", List.of(10, 11));

        mockMvc.perform(put("/api/profile/subjects")
                        .session(anonSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Not authenticated"));
    }

    @Test
    void updateSubjects_emptyList_returns200() throws Exception {
        User updated = new User();
        updated.setId(2L);
        updated.setUsername("alice");
        updated.setEmail("alice@example.com");
        updated.setRole(new Role("student"));
        updated.setName("Alice");
        updated.setAge(22);

        when(userService.updateSubjects(eq(student), eq(Set.of()))).thenReturn(updated);

        Map<String, Object> body = Map.of("subjectIds", List.of());

        mockMvc.perform(put("/api/profile/subjects")
                        .session(authSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects.length()").value(0));
    }

}
