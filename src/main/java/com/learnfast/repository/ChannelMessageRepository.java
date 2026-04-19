package com.learnfast.repository;

import com.learnfast.model.Channel;
import com.learnfast.model.ChannelMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChannelMessageRepository extends JpaRepository<ChannelMessage, Long> {
    List<ChannelMessage> findByChannelOrderBySentAtAsc(Channel channel);
}
