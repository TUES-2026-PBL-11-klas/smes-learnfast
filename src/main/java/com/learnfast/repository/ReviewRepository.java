package com.learnfast.repository;

import com.learnfast.model.Review;
import com.learnfast.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByMentorOrderByCreatedAtDesc(User mentor);
    boolean existsByStudentAndMentor(User student, User mentor);
}
