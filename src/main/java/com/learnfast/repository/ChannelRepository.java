package com.learnfast.repository;

import com.learnfast.model.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

    @Query("SELECT DISTINCT c FROM Channel c LEFT JOIN c.members m " +
           "WHERE c.creator.id = :userId OR m.id = :userId")
    List<Channel> findByMemberOrCreator(@Param("userId") Long userId);
}
