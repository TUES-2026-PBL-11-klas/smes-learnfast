package com.learnfast.repository;

import com.learnfast.model.MentorSession;
import com.learnfast.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SessionRepository extends JpaRepository<MentorSession, Long> {
    List<MentorSession> findByStudentOrderByCreatedAtDesc(User student);
    List<MentorSession> findByMentorOrderByCreatedAtDesc(User mentor);
    List<MentorSession> findByStudentOrMentorOrderByCreatedAtDesc(User student, User mentor);
}
