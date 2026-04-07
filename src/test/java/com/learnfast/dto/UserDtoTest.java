package com.learnfast.dto;

import com.learnfast.model.Review;
import com.learnfast.model.Role;
import com.learnfast.model.Subject;
import com.learnfast.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class UserDtoTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    private User buildUser(String roleName) {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setRole(new Role(roleName));
        user.setName("Alice");
        user.setAge(28);
        user.setBio("Bio text");
        user.setAvatarUrl("https://example.com/alice.png");
        return user;
    }

    private Review reviewWithRating(int rating) {
        Review r = new Review();
        r.setRating(rating);
        return r;
    }

    // ── from() — basic field mapping ──────────────────────────────────────────

    @Test
    void from_mapsBasicFields() {
        User user = buildUser("student");
        UserDto dto = UserDto.from(user);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getUsername()).isEqualTo("alice");
        assertThat(dto.getEmail()).isEqualTo("alice@example.com");
        assertThat(dto.getRole()).isEqualTo("student");
        assertThat(dto.getName()).isEqualTo("Alice");
        assertThat(dto.getAge()).isEqualTo(28);
        assertThat(dto.getBio()).isEqualTo("Bio text");
        assertThat(dto.getAvatarUrl()).isEqualTo("https://example.com/alice.png");
    }

    @Test
    void from_mapsSubjectNames() {
        User user = buildUser("mentor");
        Subject math = new Subject("Math");
        Subject science = new Subject("Science");
        user.setSubjects(Set.of(math, science));

        UserDto dto = UserDto.from(user);

        assertThat(dto.getSubjects()).containsExactlyInAnyOrder("Math", "Science");
    }

    @Test
    void from_emptySubjects_returnsEmptySet() {
        User user = buildUser("student");
        // subjects default to empty HashSet in User

        UserDto dto = UserDto.from(user);

        assertThat(dto.getSubjects()).isEmpty();
    }

    // ── from() — rating / review logic ───────────────────────────────────────

    @Test
    void from_mentor_withNoReviews_returnsZeroRatingAndCount() {
        User mentor = buildUser("mentor");
        // reviews default to empty set

        UserDto dto = UserDto.from(mentor);

        assertThat(dto.getReviewCount()).isEqualTo(0);
        assertThat(dto.getAverageRating()).isEqualTo(0.0);
    }

    @Test
    void from_mentor_withOneReview_returnsCorrectAverage() {
        User mentor = buildUser("mentor");
        mentor.setReviews(Set.of(reviewWithRating(4)));

        UserDto dto = UserDto.from(mentor);

        assertThat(dto.getReviewCount()).isEqualTo(1);
        assertThat(dto.getAverageRating()).isEqualTo(4.0);
    }

    @Test
    void from_mentor_withMultipleReviews_returnsRoundedAverage() {
        User mentor = buildUser("mentor");
        // ratings: 4, 5, 3 → sum = 12, avg = 4.0
        mentor.setReviews(Set.of(
                reviewWithRating(4),
                reviewWithRating(5),
                reviewWithRating(3)
        ));

        UserDto dto = UserDto.from(mentor);

        assertThat(dto.getReviewCount()).isEqualTo(3);
        assertThat(dto.getAverageRating()).isEqualTo(4.0);
    }

    @Test
    void from_mentor_averageIsRoundedToOneDecimalPlace() {
        User mentor = buildUser("mentor");
        // ratings: 4, 5 → sum = 9, avg = 4.5
        mentor.setReviews(Set.of(
                reviewWithRating(4),
                reviewWithRating(5)
        ));

        UserDto dto = UserDto.from(mentor);

        assertThat(dto.getAverageRating()).isEqualTo(4.5);
    }

    @Test
    void from_mentor_averageRoundedCorrectly_forRepeatingDecimal() {
        User mentor = buildUser("mentor");
        // ratings: 1, 1, 2 → sum = 4, avg = 1.333... → rounded to 1.3
        mentor.setReviews(Set.of(
                reviewWithRating(1),
                reviewWithRating(1),
                reviewWithRating(2)
        ));

        UserDto dto = UserDto.from(mentor);

        assertThat(dto.getAverageRating()).isEqualTo(1.3);
    }

    @Test
    void from_student_alwaysReturnsZeroRatingAndCount_regardlessOfReviews() {
        // students cannot be reviewed — the DTO logic gates on role name
        User student = buildUser("student");
        Review r = reviewWithRating(5);
        student.setReviews(Set.of(r));

        UserDto dto = UserDto.from(student);

        assertThat(dto.getReviewCount()).isEqualTo(0);
        assertThat(dto.getAverageRating()).isEqualTo(0.0);
    }

    @Test
    void from_mentor_allFiveStarReviews_returnsAverageFive() {
        User mentor = buildUser("mentor");
        mentor.setReviews(Set.of(
                reviewWithRating(5),
                reviewWithRating(5),
                reviewWithRating(5)
        ));

        UserDto dto = UserDto.from(mentor);

        assertThat(dto.getAverageRating()).isEqualTo(5.0);
        assertThat(dto.getReviewCount()).isEqualTo(3);
    }

    @Test
    void from_mentor_allOneStarReviews_returnsAverageOne() {
        User mentor = buildUser("mentor");
        mentor.setReviews(Set.of(
                reviewWithRating(1),
                reviewWithRating(1)
        ));

        UserDto dto = UserDto.from(mentor);

        assertThat(dto.getAverageRating()).isEqualTo(1.0);
        assertThat(dto.getReviewCount()).isEqualTo(2);
    }

    // ── getters (no setters on UserDto — all set via from()) ─────────────────

    @Test
    void from_returnsNewInstanceEachCall() {
        User user = buildUser("student");
        UserDto dto1 = UserDto.from(user);
        UserDto dto2 = UserDto.from(user);

        assertThat(dto1).isNotSameAs(dto2);
        assertThat(dto1.getId()).isEqualTo(dto2.getId());
    }

    @Test
    void from_nullBioAndAvatarUrl_arePreserved() {
        User user = buildUser("student");
        user.setBio(null);
        user.setAvatarUrl(null);

        UserDto dto = UserDto.from(user);

        assertThat(dto.getBio()).isNull();
        assertThat(dto.getAvatarUrl()).isNull();
    }
}
