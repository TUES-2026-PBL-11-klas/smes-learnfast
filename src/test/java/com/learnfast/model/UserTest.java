package com.learnfast.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class UserTest {

    private Role studentRole;
    private Role mentorRole;

    @BeforeEach
    void setUp() {
        studentRole = new Role("student");
        mentorRole = new Role("mentor");
    }

    @Test
    void defaultConstructor_createsInstance() {
        User user = new User();
        assertThat(user).isNotNull();
    }

    @Test
    void subjects_defaultsToEmptySet() {
        User user = new User();
        assertThat(user.getSubjects()).isNotNull().isEmpty();
    }

    @Test
    void reviews_defaultsToEmptySet() {
        User user = new User();
        assertThat(user.getReviews()).isNotNull().isEmpty();
    }

    @Test
    void setAndGetId() {
        User user = new User();
        user.setId(1L);
        assertThat(user.getId()).isEqualTo(1L);
    }

    @Test
    void setAndGetUsername() {
        User user = new User();
        user.setUsername("alice");
        assertThat(user.getUsername()).isEqualTo("alice");
    }

    @Test
    void setAndGetEmail() {
        User user = new User();
        user.setEmail("alice@example.com");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void setAndGetPassword() {
        User user = new User();
        user.setPassword("hashed_password");
        assertThat(user.getPassword()).isEqualTo("hashed_password");
    }

    @Test
    void setAndGetRole() {
        User user = new User();
        user.setRole(studentRole);
        assertThat(user.getRole()).isEqualTo(studentRole);
        assertThat(user.getRole().getName()).isEqualTo("student");
    }

    @Test
    void setAndGetName() {
        User user = new User();
        user.setName("Alice Smith");
        assertThat(user.getName()).isEqualTo("Alice Smith");
    }

    @Test
    void setAndGetAge() {
        User user = new User();
        user.setAge(28);
        assertThat(user.getAge()).isEqualTo(28);
    }

    @Test
    void setAndGetBio() {
        User user = new User();
        user.setBio("Java developer and mentor.");
        assertThat(user.getBio()).isEqualTo("Java developer and mentor.");
    }

    @Test
    void setAndGetAvatarUrl() {
        User user = new User();
        user.setAvatarUrl("https://example.com/avatar.png");
        assertThat(user.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
    }

    @Test
    void setAndGetSubjects() {
        User user = new User();
        Subject math = new Subject("Math");
        math.setId(1L);
        Subject science = new Subject("Science");
        science.setId(2L);

        user.setSubjects(Set.of(math, science));

        assertThat(user.getSubjects()).hasSize(2).containsExactlyInAnyOrder(math, science);
    }

    @Test
    void setAndGetReviews() {
        User user = new User();
        Review r1 = new Review();
        r1.setId(1L);
        Review r2 = new Review();
        r2.setId(2L);

        user.setReviews(Set.of(r1, r2));

        assertThat(user.getReviews()).hasSize(2).containsExactlyInAnyOrder(r1, r2);
    }

    @Test
    void setSubjects_replacesExistingSet() {
        User user = new User();
        user.setSubjects(Set.of(new Subject("Old")));

        Subject newSubject = new Subject("New");
        user.setSubjects(Set.of(newSubject));

        assertThat(user.getSubjects()).containsExactly(newSubject);
    }

    @Test
    void setSubjects_withEmptySet() {
        User user = new User();
        user.setSubjects(new HashSet<>());
        assertThat(user.getSubjects()).isEmpty();
    }

    @Test
    void role_canBeChangedFromStudentToMentor() {
        User user = new User();
        user.setRole(studentRole);
        assertThat(user.getRole().getName()).isEqualTo("student");

        user.setRole(mentorRole);
        assertThat(user.getRole().getName()).isEqualTo("mentor");
    }

    @Test
    void bio_canBeNull() {
        User user = new User();
        user.setBio(null);
        assertThat(user.getBio()).isNull();
    }

    @Test
    void avatarUrl_canBeNull() {
        User user = new User();
        user.setAvatarUrl(null);
        assertThat(user.getAvatarUrl()).isNull();
    }

    @Test
    void fullyPopulatedUser_hasAllFieldsSet() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPassword("secret");
        user.setRole(mentorRole);
        user.setName("Alice");
        user.setAge(30);
        user.setBio("Experienced mentor");
        user.setAvatarUrl("https://example.com/alice.png");

        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getPassword()).isEqualTo("secret");
        assertThat(user.getRole().getName()).isEqualTo("mentor");
        assertThat(user.getName()).isEqualTo("Alice");
        assertThat(user.getAge()).isEqualTo(30);
        assertThat(user.getBio()).isEqualTo("Experienced mentor");
        assertThat(user.getAvatarUrl()).isEqualTo("https://example.com/alice.png");
    }
}
