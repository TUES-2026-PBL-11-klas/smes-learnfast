package com.learnfast;

import com.learnfast.model.Role;
import com.learnfast.service.SessionService;
import com.learnfast.dto.SessionDto;
import com.learnfast.model.MentorSession;
import com.learnfast.model.User;
import com.learnfast.repository.SessionRepository;
import com.learnfast.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SessionService sessionService;

    private User student;
    private User mentor;
    private MentorSession session;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1L);
        student.setName("Alice");
        student.setUsername("alice");
        mentor = new User();
        mentor.setId(2L);
        mentor.setName("Bob");
        mentor.setUsername("bob");

        session = new MentorSession();
        session.setId(100L);
        session.setStudent(student);
        session.setMentor(mentor);
        session.setStatus(MentorSession.Status.PENDING);
        session.setRoomId(UUID.randomUUID().toString());
        session.setCreatedAt(LocalDateTime.of(2024, 4, 1, 8, 0));
    }

    // ─── createSession ─────────────────────────────────────────────────────────

    @Test
    void createSession_success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(mentor));
        when(sessionRepository.save(any(MentorSession.class))).thenAnswer(inv -> inv.getArgument(0));

        MentorSession result = sessionService.createSession(student, 2L);

        assertThat(result.getStudent()).isEqualTo(student);
        assertThat(result.getMentor()).isEqualTo(mentor);
        assertThat(result.getStatus()).isEqualTo(MentorSession.Status.PENDING);
        assertThat(result.getRoomId()).isNotBlank();

        // room ID should be a valid UUID
        assertThatCode(() -> UUID.fromString(result.getRoomId())).doesNotThrowAnyException();

        verify(sessionRepository).save(any(MentorSession.class));
    }

    @Test
    void createSession_generatesDifferentRoomIdsForEachSession() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(mentor));
        when(sessionRepository.save(any(MentorSession.class))).thenAnswer(inv -> inv.getArgument(0));

        MentorSession s1 = sessionService.createSession(student, 2L);
        MentorSession s2 = sessionService.createSession(student, 2L);

        assertThat(s1.getRoomId()).isNotEqualTo(s2.getRoomId());
    }

    @Test
    void createSession_throwsWhenMentorNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.createSession(student, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Mentor not found");

        verify(sessionRepository, never()).save(any());
    }

    // ─── acceptSession ─────────────────────────────────────────────────────────

    @Test
    void acceptSession_success() {
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(MentorSession.class))).thenAnswer(inv -> inv.getArgument(0));

        MentorSession result = sessionService.acceptSession(100L, mentor);

        assertThat(result.getStatus()).isEqualTo(MentorSession.Status.ACCEPTED);
        verify(sessionRepository).save(session);
    }

    @Test
    void acceptSession_throwsWhenSessionNotFound() {
        when(sessionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.acceptSession(999L, mentor))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Session not found");
    }

    @Test
    void acceptSession_throwsWhenCalledByWrongMentor() {
        User otherMentor = new User();
        otherMentor.setId(99L);

        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> sessionService.acceptSession(100L, otherMentor))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Not authorized");

        verify(sessionRepository, never()).save(any());
    }

    // ─── rejectSession ─────────────────────────────────────────────────────────

    @Test
    void rejectSession_success() {
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(MentorSession.class))).thenAnswer(inv -> inv.getArgument(0));

        MentorSession result = sessionService.rejectSession(100L, mentor);

        assertThat(result.getStatus()).isEqualTo(MentorSession.Status.REJECTED);
        verify(sessionRepository).save(session);
    }

    @Test
    void rejectSession_throwsWhenSessionNotFound() {
        when(sessionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.rejectSession(999L, mentor))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Session not found");
    }

    @Test
    void rejectSession_throwsWhenCalledByWrongMentor() {
        User otherMentor = new User();
        otherMentor.setId(99L);

        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> sessionService.rejectSession(100L, otherMentor))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Not authorized");

        verify(sessionRepository, never()).save(any());
    }

    // ─── getUserSessions ───────────────────────────────────────────────────────

    @Test
    void getUserSessions_returnsEmptyListWhenNoSessions() {
        when(sessionRepository.findByStudentOrMentorOrderByCreatedAtDesc(student, student))
                .thenReturn(List.of());

        List<SessionDto> result = sessionService.getUserSessions(student);

        assertThat(result).isEmpty();
    }
}
