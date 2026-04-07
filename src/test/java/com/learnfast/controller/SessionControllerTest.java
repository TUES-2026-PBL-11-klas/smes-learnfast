package com.learnfast.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnfast.dto.SessionDto;
import com.learnfast.dto.UserDto;
import com.learnfast.model.MentorSession;
import com.learnfast.model.Role;
import com.learnfast.model.User;
import com.learnfast.service.AuthService;
import com.learnfast.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionController.class)
class SessionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean SessionService sessionService;
    @MockBean AuthService authService;

    private User student;
    private User mentor;
    private MentorSession pendingSession;
    private SessionDto sessionDto;
    private MockHttpSession studentSession;
    private MockHttpSession mentorSession;
    private MockHttpSession anonSession;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1L);
        student.setUsername("alice");
        student.setEmail("alice@example.com");
        student.setRole(new Role("student"));
        student.setName("Alice");
        student.setAge(22);

        mentor = new User();
        mentor.setId(2L);
        mentor.setUsername("bob");
        mentor.setEmail("bob@example.com");
        mentor.setRole(new Role("mentor"));
        mentor.setName("Bob");
        mentor.setAge(35);

        pendingSession = new MentorSession();
        pendingSession.setId(100L);
        pendingSession.setStudent(student);
        pendingSession.setMentor(mentor);
        pendingSession.setStatus(MentorSession.Status.PENDING);
        pendingSession.setRoomId("room-xyz");
        pendingSession.setCreatedAt(LocalDateTime.of(2024, 4, 1, 10, 0));

        sessionDto = new SessionDto();
        sessionDto.setId(100L);
        sessionDto.setStudent(UserDto.from(student));
        sessionDto.setMentor(UserDto.from(mentor));
        sessionDto.setStatus("PENDING");
        sessionDto.setRoomId("room-xyz");
        sessionDto.setCreatedAt(LocalDateTime.of(2024, 4, 1, 10, 0));

        studentSession = new MockHttpSession();
        studentSession.setAttribute("userId", 1L);

        mentorSession = new MockHttpSession();
        mentorSession.setAttribute("userId", 2L);

        anonSession = new MockHttpSession();

        when(authService.findById(1L)).thenReturn(Optional.of(student));
        when(authService.findById(2L)).thenReturn(Optional.of(mentor));
    }

    // ── POST /api/sessions ────────────────────────────────────────────────────

    @Test
    void createSession_success_returns200() throws Exception {
        when(sessionService.createSession(eq(student), eq(2L))).thenReturn(pendingSession);
        when(sessionService.toDto(pendingSession)).thenReturn(sessionDto);

        mockMvc.perform(post("/api/sessions")
                        .session(studentSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("mentorId", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.roomId").value("room-xyz"));
    }

    @Test
    void createSession_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .session(anonSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("mentorId", 2))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Not authenticated"));
    }

    // ── GET /api/sessions ─────────────────────────────────────────────────────

    @Test
    void getSessions_authenticated_returns200() throws Exception {
        when(sessionService.getUserSessions(student)).thenReturn(List.of(sessionDto));

        mockMvc.perform(get("/api/sessions").session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(100));
    }

    @Test
    void getSessions_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/sessions").session(anonSession))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Not authenticated"));
    }

    @Test
    void getSessions_returnsEmptyList_whenNoSessions() throws Exception {
        when(sessionService.getUserSessions(student)).thenReturn(List.of());

        mockMvc.perform(get("/api/sessions").session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── PUT /api/sessions/{id}/accept ─────────────────────────────────────────

    @Test
    void acceptSession_success_returns200() throws Exception {
        MentorSession accepted = new MentorSession();
        accepted.setId(100L);
        accepted.setStudent(student);
        accepted.setMentor(mentor);
        accepted.setStatus(MentorSession.Status.ACCEPTED);
        accepted.setRoomId("room-xyz");

        SessionDto acceptedDto = new SessionDto();
        acceptedDto.setId(100L);
        acceptedDto.setStatus("ACCEPTED");

        when(sessionService.acceptSession(100L, mentor)).thenReturn(accepted);
        when(sessionService.toDto(accepted)).thenReturn(acceptedDto);

        mockMvc.perform(put("/api/sessions/100/accept").session(mentorSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void acceptSession_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/api/sessions/100/accept").session(anonSession))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Not authenticated"));
    }



    // ── PUT /api/sessions/{id}/reject ─────────────────────────────────────────

    @Test
    void rejectSession_success_returns200() throws Exception {
        MentorSession rejected = new MentorSession();
        rejected.setId(100L);
        rejected.setStudent(student);
        rejected.setMentor(mentor);
        rejected.setStatus(MentorSession.Status.REJECTED);
        rejected.setRoomId("room-xyz");

        SessionDto rejectedDto = new SessionDto();
        rejectedDto.setId(100L);
        rejectedDto.setStatus("REJECTED");

        when(sessionService.rejectSession(100L, mentor)).thenReturn(rejected);
        when(sessionService.toDto(rejected)).thenReturn(rejectedDto);

        mockMvc.perform(put("/api/sessions/100/reject").session(mentorSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void rejectSession_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/api/sessions/100/reject").session(anonSession))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Not authenticated"));
    }
}
