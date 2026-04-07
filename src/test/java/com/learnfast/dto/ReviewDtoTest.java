package com.learnfast.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class ReviewDtoTest {

    @Test
    void defaultConstructor_createsInstance() {
        ReviewDto dto = new ReviewDto();
        assertThat(dto).isNotNull();
    }

    @Test
    void allFieldsNullByDefault() {
        ReviewDto dto = new ReviewDto();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getStudentId()).isNull();
        assertThat(dto.getStudentName()).isNull();
        assertThat(dto.getStudentAvatarUrl()).isNull();
        assertThat(dto.getRating()).isNull();
        assertThat(dto.getComment()).isNull();
        assertThat(dto.getCreatedAt()).isNull();
    }

    @Test
    void setAndGetId() {
        ReviewDto dto = new ReviewDto();
        dto.setId(5L);
        assertThat(dto.getId()).isEqualTo(5L);
    }

    @Test
    void setAndGetStudentId() {
        ReviewDto dto = new ReviewDto();
        dto.setStudentId(42L);
        assertThat(dto.getStudentId()).isEqualTo(42L);
    }

    @Test
    void setAndGetStudentName() {
        ReviewDto dto = new ReviewDto();
        dto.setStudentName("Alice");
        assertThat(dto.getStudentName()).isEqualTo("Alice");
    }

    @Test
    void setAndGetStudentAvatarUrl() {
        ReviewDto dto = new ReviewDto();
        dto.setStudentAvatarUrl("https://example.com/avatar.png");
        assertThat(dto.getStudentAvatarUrl()).isEqualTo("https://example.com/avatar.png");
    }

    @Test
    void setAndGetRating() {
        ReviewDto dto = new ReviewDto();
        dto.setRating(4);
        assertThat(dto.getRating()).isEqualTo(4);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void rating_acceptsAllValidValues(int rating) {
        ReviewDto dto = new ReviewDto();
        dto.setRating(rating);
        assertThat(dto.getRating()).isEqualTo(rating);
    }

    @Test
    void setAndGetComment() {
        ReviewDto dto = new ReviewDto();
        dto.setComment("Great mentor!");
        assertThat(dto.getComment()).isEqualTo("Great mentor!");
    }

    @Test
    void comment_canBeNull() {
        ReviewDto dto = new ReviewDto();
        dto.setComment(null);
        assertThat(dto.getComment()).isNull();
    }

    @Test
    void setAndGetCreatedAt() {
        ReviewDto dto = new ReviewDto();
        LocalDateTime time = LocalDateTime.of(2024, 3, 15, 14, 30);
        dto.setCreatedAt(time);
        assertThat(dto.getCreatedAt()).isEqualTo(time);
    }

    @Test
    void fullyPopulatedDto_hasAllFieldsSet() {
        ReviewDto dto = new ReviewDto();
        LocalDateTime time = LocalDateTime.of(2024, 3, 15, 14, 30);

        dto.setId(1L);
        dto.setStudentId(10L);
        dto.setStudentName("Alice");
        dto.setStudentAvatarUrl("https://example.com/alice.png");
        dto.setRating(5);
        dto.setComment("Excellent session!");
        dto.setCreatedAt(time);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getStudentId()).isEqualTo(10L);
        assertThat(dto.getStudentName()).isEqualTo("Alice");
        assertThat(dto.getStudentAvatarUrl()).isEqualTo("https://example.com/alice.png");
        assertThat(dto.getRating()).isEqualTo(5);
        assertThat(dto.getComment()).isEqualTo("Excellent session!");
        assertThat(dto.getCreatedAt()).isEqualTo(time);
    }
}
