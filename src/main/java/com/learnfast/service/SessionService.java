package com.learnfast.service;

import com.learnfast.dto.SessionDto;
import com.learnfast.dto.UserDto;
import com.learnfast.model.MentorSession;
import com.learnfast.model.User;
import com.learnfast.repository.SessionRepository;
import com.learnfast.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    public SessionService(SessionRepository sessionRepository, UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    public MentorSession createSession(User student, Long mentorId) {
        User mentor = userRepository.findById(mentorId)
            .orElseThrow(() -> new RuntimeException("Mentor not found"));

        MentorSession session = new MentorSession();
        session.setStudent(student);
        session.setMentor(mentor);
        session.setStatus(MentorSession.Status.PENDING);
        session.setRoomId(UUID.randomUUID().toString());

        return sessionRepository.save(session);
    }

    public MentorSession acceptSession(Long sessionId, User mentor) {
        MentorSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));
        if (!session.getMentor().getId().equals(mentor.getId())) {
            throw new RuntimeException("Not authorized");
        }
        session.setStatus(MentorSession.Status.ACCEPTED);
        return sessionRepository.save(session);
    }

    public MentorSession rejectSession(Long sessionId, User mentor) {
        MentorSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));
        if (!session.getMentor().getId().equals(mentor.getId())) {
            throw new RuntimeException("Not authorized");
        }
        session.setStatus(MentorSession.Status.REJECTED);
        return sessionRepository.save(session);
    }

    public List<SessionDto> getUserSessions(User user) {
        return sessionRepository.findByStudentOrMentorOrderByCreatedAtDesc(user, user)
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    public SessionDto toDto(MentorSession s) {
        SessionDto dto = new SessionDto();
        dto.setId(s.getId());
        dto.setStudent(UserDto.from(s.getStudent()));
        dto.setMentor(UserDto.from(s.getMentor()));
        dto.setStatus(s.getStatus().name());
        dto.setCreatedAt(s.getCreatedAt());
        dto.setRoomId(s.getRoomId());
        return dto;
    }
}
