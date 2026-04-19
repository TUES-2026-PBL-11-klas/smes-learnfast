package com.learnfast.repository;

import com.learnfast.model.MentorSession;
import com.learnfast.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SessionRepository extends JpaRepository<MentorSession, Long> {
    List<MentorSession> findByStudentOrderByCreatedAtDesc(User student);
    List<MentorSession> findByMentorOrderByCreatedAtDesc(User mentor);
    List<MentorSession> findByStudentOrMentorOrderByCreatedAtDesc(User student, User mentor);

    /** True if an ACCEPTED session exists between student and mentor (either direction). */
    @Query("SELECT COUNT(s) > 0 FROM MentorSession s WHERE s.status = 'ACCEPTED' AND " +
           "((s.student.id = :a AND s.mentor.id = :b) OR (s.student.id = :b AND s.mentor.id = :a))")
    boolean hasAcceptedSession(@Param("a") Long userA, @Param("b") Long userB);

    /** Any session (any status) between student and mentor. */
    @Query("SELECT s FROM MentorSession s WHERE " +
           "((s.student.id = :a AND s.mentor.id = :b) OR (s.student.id = :b AND s.mentor.id = :a)) " +
           "ORDER BY s.createdAt DESC")
    List<MentorSession> findBetween(@Param("a") Long userA, @Param("b") Long userB);
}
