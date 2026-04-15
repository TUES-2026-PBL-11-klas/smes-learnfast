package com.learnfast.service;


import com.learnfast.dto.ReviewDto;
import com.learnfast.model.Review;
import com.learnfast.model.Role;
import com.learnfast.model.User;
import com.learnfast.repository.ReviewRepository;
import com.learnfast.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User student;
    private User mentor;
    private Review review;

    @BeforeEach
    void setUp() {
        Role studentRole = new Role();
        studentRole.setName("student");

        Role mentorRole = new Role();
        mentorRole.setName("mentor");

        student = new User();
        student.setId(1L);
        student.setName("Alice");
        student.setRole(studentRole);
        student.setAvatarUrl("https://example.com/alice.png");

        mentor = new User();
        mentor.setId(2L);
        mentor.setName("Bob");
        mentor.setRole(mentorRole);

        review = new Review();
        review.setId(10L);
        review.setStudent(student);
        review.setMentor(mentor);
        review.setRating(5);
        review.setComment("Excellent!");
        review.setCreatedAt(LocalDateTime.of(2024, 3, 1, 9, 0));
    }

    // ─── addReview ─────────────────────────────────────────────────────────────

    @Test
    void addReview_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findById(2L)).thenReturn(Optional.of(mentor));
        when(reviewRepository.existsByStudentAndMentor(student, mentor)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        Review result = reviewService.addReview(1L, 2L, 5, "Excellent!");

        assertThat(result.getStudent()).isEqualTo(student);
        assertThat(result.getMentor()).isEqualTo(mentor);
        assertThat(result.getRating()).isEqualTo(5);
        assertThat(result.getComment()).isEqualTo("Excellent!");
        assertThat(result.getCreatedAt()).isNotNull();

        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void addReview_throwsWhenStudentNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.addReview(99L, 2L, 4, "Good"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Student with id 99 not found");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void addReview_throwsWhenMentorNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.addReview(1L, 99L, 4, "Good"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Mentor with id 99 not found");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void addReview_throwsWhenTargetIsNotMentor() {
        // mentor object actually has "student" role to trigger the guard
        Role studentRole = new Role();
        studentRole.setName("student");
        mentor.setRole(studentRole);

        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findById(2L)).thenReturn(Optional.of(mentor));

        assertThatThrownBy(() -> reviewService.addReview(1L, 2L, 4, "Good"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Target user is not a mentor");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void addReview_throwsWhenDuplicateReview() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findById(2L)).thenReturn(Optional.of(mentor));
        when(reviewRepository.existsByStudentAndMentor(student, mentor)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.addReview(1L, 2L, 5, "Again"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already reviewed");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void addReview_throwsWhenRatingIsNull() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findById(2L)).thenReturn(Optional.of(mentor));
        when(reviewRepository.existsByStudentAndMentor(student, mentor)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.addReview(1L, 2L, null, "No rating"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Rating must be between 1 and 5");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 6, 100})
    void addReview_throwsWhenRatingOutOfRange(int invalidRating) {
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findById(2L)).thenReturn(Optional.of(mentor));
        when(reviewRepository.existsByStudentAndMentor(student, mentor)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.addReview(1L, 2L, invalidRating, "Bad"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Rating must be between 1 and 5");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void addReview_acceptsAllValidRatings(int validRating) {
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findById(2L)).thenReturn(Optional.of(mentor));
        when(reviewRepository.existsByStudentAndMentor(student, mentor)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        Review result = reviewService.addReview(1L, 2L, validRating, "OK");

        assertThat(result.getRating()).isEqualTo(validRating);
    }

    // ─── getReviewsForMentor ───────────────────────────────────────────────────

    @Test
    void getReviewsForMentor_returnsDtos() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(mentor));
        when(reviewRepository.findByMentorOrderByCreatedAtDesc(mentor)).thenReturn(List.of(review));

        List<ReviewDto> result = reviewService.getReviewsForMentor(2L);

        assertThat(result).hasSize(1);
        ReviewDto dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getStudentId()).isEqualTo(1L);
        assertThat(dto.getStudentName()).isEqualTo("Alice");
        assertThat(dto.getStudentAvatarUrl()).isEqualTo("https://example.com/alice.png");
        assertThat(dto.getRating()).isEqualTo(5);
        assertThat(dto.getComment()).isEqualTo("Excellent!");
        assertThat(dto.getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 3, 1, 9, 0));
    }

    @Test
    void getReviewsForMentor_throwsWhenMentorNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReviewsForMentor(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Mentor with id 99 not found");
    }

    @Test
    void getReviewsForMentor_returnsEmptyListWhenNoReviews() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(mentor));
        when(reviewRepository.findByMentorOrderByCreatedAtDesc(mentor)).thenReturn(List.of());

        List<ReviewDto> result = reviewService.getReviewsForMentor(2L);

        assertThat(result).isEmpty();
    }
}
