package com.learnfast.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnfast.dto.ReviewDto;
import com.learnfast.service.ReviewService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ReviewService reviewService;

    private MockHttpSession authSession;
    private MockHttpSession anonSession;

    @BeforeEach
    void setUp() {
        authSession = new MockHttpSession();
        authSession.setAttribute("userId", 1L);

        anonSession = new MockHttpSession();
    }

    // ── POST /api/reviews ─────────────────────────────────────────────────────


    @Test
    void addReview_unauthenticated_returns401() throws Exception {
        Map<String, Object> body = Map.of("mentorId", 2, "rating", 5, "comment", "Great!");

        mockMvc.perform(post("/api/reviews")
                        .session(anonSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Not authenticated"));
    }

    @Test
    void addReview_duplicateReview_returns400() throws Exception {
        doThrow(new RuntimeException("You have already reviewed this mentor"))
                .when(reviewService).addReview(any(), any(), any(), any());

        Map<String, Object> body = Map.of("mentorId", 2, "rating", 4, "comment", "Again");

        mockMvc.perform(post("/api/reviews")
                        .session(authSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("You have already reviewed this mentor"));
    }

    @Test
    void addReview_invalidRating_returns400() throws Exception {
        doThrow(new RuntimeException("Rating must be between 1 and 5"))
                .when(reviewService).addReview(any(), any(), any(), any());

        Map<String, Object> body = Map.of("mentorId", 2, "rating", 10, "comment", "Bad");

        mockMvc.perform(post("/api/reviews")
                        .session(authSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Rating must be between 1 and 5"));
    }

    @Test
    void addReview_targetIsNotMentor_returns400() throws Exception {
        doThrow(new RuntimeException("Target user is not a mentor"))
                .when(reviewService).addReview(any(), any(), any(), any());

        Map<String, Object> body = Map.of("mentorId", 3, "rating", 5, "comment", "Oops");

        mockMvc.perform(post("/api/reviews")
                        .session(authSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Target user is not a mentor"));
    }


    // ── GET /api/reviews/mentor/{mentorId} ────────────────────────────────────

    @Test
    void getReviews_returnsListOfReviewDtos() throws Exception {
        ReviewDto dto = new ReviewDto();
        dto.setId(10L);
        dto.setStudentId(1L);
        dto.setStudentName("Alice");
        dto.setRating(5);
        dto.setComment("Excellent!");
        dto.setCreatedAt(LocalDateTime.of(2024, 3, 1, 9, 0));

        when(reviewService.getReviewsForMentor(2L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/reviews/mentor/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].studentName").value("Alice"))
                .andExpect(jsonPath("$[0].rating").value(5))
                .andExpect(jsonPath("$[0].comment").value("Excellent!"));
    }

    @Test
    void getReviews_noAuth_stillReturns200() throws Exception {
        when(reviewService.getReviewsForMentor(2L)).thenReturn(List.of());

        mockMvc.perform(get("/api/reviews/mentor/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
