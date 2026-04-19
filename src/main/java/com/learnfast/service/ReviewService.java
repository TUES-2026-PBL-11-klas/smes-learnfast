package com.learnfast.service;

import com.learnfast.dto.ReviewDto;
import com.learnfast.exception.BadRequestException;
import com.learnfast.exception.LearnFastException;
import com.learnfast.exception.ResourceNotFoundException;
import com.learnfast.model.Review;
import com.learnfast.model.User;
import com.learnfast.repository.ReviewRepository;
import com.learnfast.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    public Review addReview(Long studentId, Long mentorId, Integer rating, String comment) {
        User student = userRepository.findById(studentId).orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        User mentor = userRepository.findById(mentorId).orElseThrow(() -> new ResourceNotFoundException("Mentor", mentorId));

        if (!"mentor".equals(mentor.getRole().getName())) {
            throw new BadRequestException("Target user is not a mentor");
        }

        if (reviewRepository.existsByStudentAndMentor(student, mentor)) {
            throw new BadRequestException("You have already reviewed this mentor");
        }

        if (rating == null || rating < 1 || rating > 5) {
            throw new BadRequestException("Rating must be between 1 and 5");
        }

        Review review = new Review();
        review.setStudent(student);
        review.setMentor(mentor);
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(LocalDateTime.now());

        return reviewRepository.save(review);
    }

    public List<ReviewDto> getReviewsForMentor(Long mentorId) {
        User mentor = userRepository.findById(mentorId).orElseThrow(() -> new ResourceNotFoundException("Mentor", mentorId));
        return reviewRepository.findByMentorOrderByCreatedAtDesc(mentor)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Generates a CSV summary of all reviews for a mentor.
     * Demonstrates try-with-resources with multiple AutoCloseable resources.
     */
    public String generateMentorReportCsv(Long mentorId) {
        User mentor = userRepository.findById(mentorId)
            .orElseThrow(() -> new ResourceNotFoundException("Mentor", mentorId));
        List<Review> reviews = reviewRepository.findByMentorOrderByCreatedAtDesc(mentor);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
            writer.write("student_id,student_name,rating,comment,created_at\n");
            for (Review r : reviews) {
                writer.write(String.format("%d,%s,%d,%s,%s%n",
                    r.getStudent().getId(),
                    r.getStudent().getName(),
                    r.getRating(),
                    r.getComment() != null ? r.getComment().replace(",", ";") : "",
                    r.getCreatedAt()));
            }
            writer.flush();
            return baos.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new LearnFastException("Failed to generate mentor report: " + e.getMessage());
        }
    }

    private ReviewDto toDto(Review review) {
        ReviewDto dto = new ReviewDto();
        dto.setId(review.getId());
        dto.setStudentId(review.getStudent().getId());
        dto.setStudentName(review.getStudent().getName());
        dto.setStudentAvatarUrl(review.getStudent().getAvatarUrl());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }
}
