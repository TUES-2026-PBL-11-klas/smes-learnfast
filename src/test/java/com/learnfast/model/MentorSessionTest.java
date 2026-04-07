package com.learnfast.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class MentorSessionTest {

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
        MentorSession session = new MentorSession();
        assertThat(session).isNotNull();
    }

    @Test
    void status_defaultsToPending() {
        MentorSession session = new MentorSession();
        assertThat(session.getStatus()).isEqualTo(MentorSession.Status.PENDING);
    }

    @Test
    void createdAt_defaultsToNow() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        MentorSession session = new MentorSession();
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertThat(session.getCreatedAt()).isBetween(before, after);
    }

    @Test
    void setAndGetId() {
        MentorSession session = new MentorSession();
        session.setId(10L);
        assertThat(session.getId()).isEqualTo(10L);
    }

    @Test
    void setAndGetStudent() {
        MentorSession session = new MentorSession();
        session.setStudent(student);
        assertThat(session.getStudent()).isEqualTo(student);
    }

    @Test
    void setAndGetMentor() {
        MentorSession session = new MentorSession();
        session.setMentor(mentor);
        assertThat(session.getMentor()).isEqualTo(mentor);
    }

    @Test
    void setAndGetRoomId() {
        MentorSession session = new MentorSession();
        session.setRoomId("room-abc-123");
        assertThat(session.getRoomId()).isEqualTo("room-abc-123");
    }

    @Test
    void setAndGetCreatedAt() {
        MentorSession session = new MentorSession();
        LocalDateTime time = LocalDateTime.of(2024, 5, 1, 10, 0);
        session.setCreatedAt(time);
        assertThat(session.getCreatedAt()).isEqualTo(time);
    }

    @Test
    void allStatusValuesAreAccessible() {
        assertThat(MentorSession.Status.values())
                .containsExactlyInAnyOrder(
                        MentorSession.Status.PENDING,
                        MentorSession.Status.ACCEPTED,
                        MentorSession.Status.REJECTED,
                        MentorSession.Status.COMPLETED
                );
    }

    @Test
    void setStatus_toAccepted() {
        MentorSession session = new MentorSession();
        session.setStatus(MentorSession.Status.ACCEPTED);
        assertThat(session.getStatus()).isEqualTo(MentorSession.Status.ACCEPTED);
    }

    @Test
    void setStatus_toRejected() {
        MentorSession session = new MentorSession();
        session.setStatus(MentorSession.Status.REJECTED);
        assertThat(session.getStatus()).isEqualTo(MentorSession.Status.REJECTED);
    }

    @Test
    void setStatus_toCompleted() {
        MentorSession session = new MentorSession();
        session.setStatus(MentorSession.Status.COMPLETED);
        assertThat(session.getStatus()).isEqualTo(MentorSession.Status.COMPLETED);
    }

    @Test
    void studentAndMentorAreIndependent() {
        MentorSession session = new MentorSession();
        session.setStudent(student);
        session.setMentor(mentor);

        assertThat(session.getStudent().getId()).isEqualTo(1L);
        assertThat(session.getMentor().getId()).isEqualTo(2L);
        assertThat(session.getStudent()).isNotEqualTo(session.getMentor());
    }
}
