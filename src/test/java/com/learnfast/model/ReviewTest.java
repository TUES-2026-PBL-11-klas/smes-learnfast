package com.learnfast.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class ReviewTest {

    private User student;
    private User mentor;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1L);
        student.setName("Alice");
        student.setRole(new Role("student"));

        mentor = new User();
        mentor.setId(2L);
        mentor.setName("Bob");
        mentor.setRole(new Role("mentor"));
    }

    @Test
    void defaultConstructor_createsInstance() {
        Review review = new Review();
        assertThat(review).isNotNull();
    }

    @Test
    void createdAt_defaultsToNow() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        Review review = new Review();
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertThat(review.getCreatedAt()).isBetween(before, after);
    }

    @Test
    void setAndGetId() {
        Review review = new Review();
        review.setId(99L);
        assertThat(review.getId()).isEqualTo(99L);
    }

    @Test
    void setAndGetStudent() {
        Review review = new Review();
        review.setStudent(student);
        assertThat(review.getStudent()).isEqualTo(student);
    }

    @Test
    void setAndGetMentor() {
        Review review = new Review();
        review.setMentor(mentor);
        assertThat(review.getMentor()).isEqualTo(mentor);
    }

    @Test
    void setAndGetRating() {
        Review review = new Review();
        review.setRating(4);
        assertThat(review.getRating()).isEqualTo(4);
    }

    @Test
    void setAndGetComment() {
        Review review = new Review();
        review.setComment("Great session!");
        assertThat(review.getComment()).isEqualTo("Great session!");
    }

    @Test
    void setAndGetCreatedAt() {
        Review review = new Review();
        LocalDateTime time = LocalDateTime.of(2024, 3, 15, 14, 30);
        review.setCreatedAt(time);
        assertThat(review.getCreatedAt()).isEqualTo(time);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void rating_acceptsAllValidValues(int rating) {
        Review review = new Review();
        review.setRating(rating);
        assertThat(review.getRating()).isEqualTo(rating);
    }

    @Test
    void comment_canBeNull() {
        Review review = new Review();
        review.setComment(null);
        assertThat(review.getComment()).isNull();
    }

    @Test
    void comment_allowsLongText() {
        Review review = new Review();
        String longComment = "A".repeat(1000);
        review.setComment(longComment);
        assertThat(review.getComment()).hasSize(1000);
    }

    @Test
    void studentAndMentorAreIndependent() {
        Review review = new Review();
        review.setStudent(student);
        review.setMentor(mentor);

        assertThat(review.getStudent().getId()).isEqualTo(1L);
        assertThat(review.getMentor().getId()).isEqualTo(2L);
        assertThat(review.getStudent()).isNotEqualTo(review.getMentor());
    }
}
