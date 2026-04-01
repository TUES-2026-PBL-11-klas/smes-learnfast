package com.learnfast.controller;

import com.learnfast.dto.ReviewDto;
import com.learnfast.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<?> addReview(@RequestBody Map<String, Object> body, HttpSession session) {
        Long studentId = (Long) session.getAttribute("userId");
        if (studentId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        try {
            Long mentorId = Long.parseLong(body.get("mentorId").toString());
            Integer rating = body.get("rating") != null ? Integer.parseInt(body.get("rating").toString()) : null;
            String comment = (String) body.get("comment");

            reviewService.addReview(studentId, mentorId, rating, comment);
            return ResponseEntity.ok(Map.of("message", "Review added successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/mentor/{mentorId}")
    public ResponseEntity<List<ReviewDto>> getReviews(@PathVariable Long mentorId) {
        return ResponseEntity.ok(reviewService.getReviewsForMentor(mentorId));
    }
}
