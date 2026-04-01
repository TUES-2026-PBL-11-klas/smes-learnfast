package com.learnfast.repository;

import com.learnfast.model.ChatMessage;
import com.learnfast.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE " +
           "(m.sender = :user1 AND m.receiver = :user2) OR " +
           "(m.sender = :user2 AND m.receiver = :user1) " +
           "ORDER BY m.sentAt ASC")
    List<ChatMessage> findConversation(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT DISTINCT m.receiver FROM ChatMessage m WHERE m.sender = :user")
    List<User> findPartnersBySender(@Param("user") User user);

    @Query("SELECT DISTINCT m.sender FROM ChatMessage m WHERE m.receiver = :user")
    List<User> findPartnersByReceiver(@Param("user") User user);
}
