package com.learnfast.repository;

import com.learnfast.model.CallRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CallRecordRepository extends JpaRepository<CallRecord, Long> {

    @Query("SELECT c FROM CallRecord c WHERE c.caller.id = :userId OR c.callee.id = :userId ORDER BY c.startedAt DESC")
    List<CallRecord> findByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM CallRecord c WHERE c.callee.id = :calleeId AND c.status = 'RINGING' AND c.startedAt >= :since ORDER BY c.startedAt DESC")
    List<CallRecord> findRecentRingingForCallee(@Param("calleeId") Long calleeId, @Param("since") LocalDateTime since);
}
