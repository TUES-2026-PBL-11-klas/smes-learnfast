package com.learnfast.service;

import com.learnfast.model.CallRecord;
import com.learnfast.model.ChatMessage;
import com.learnfast.model.User;
import com.learnfast.repository.CallRecordRepository;
import com.learnfast.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CallRecordService {

    private final CallRecordRepository callRecordRepository;
    private final UserRepository userRepository;
    private final ChatService chatService;
    private final SimpMessagingTemplate messaging;

    public CallRecordService(CallRecordRepository callRecordRepository,
                             UserRepository userRepository,
                             @Lazy ChatService chatService,
                             SimpMessagingTemplate messaging) {
        this.callRecordRepository = callRecordRepository;
        this.userRepository = userRepository;
        this.chatService = chatService;
        this.messaging = messaging;
    }

    public CallRecord createCall(Long callerId, Long calleeId, String roomId) {
        User caller = userRepository.findById(callerId).orElseThrow();
        User callee = userRepository.findById(calleeId).orElseThrow();
        CallRecord record = new CallRecord();
        record.setCaller(caller);
        record.setCallee(callee);
        record.setRoomId(roomId);
        record.setStatus(CallRecord.Status.RINGING);
        CallRecord saved = callRecordRepository.save(record);

        // Chat event: outgoing / incoming call
        pushCallEvent(saved, "RINGING:0");

        return saved;
    }

    public Optional<CallRecord> updateStatus(Long callId, CallRecord.Status status, Integer durationSeconds) {
        return callRecordRepository.findById(callId).map(record -> {
            record.setStatus(status);
            if (status == CallRecord.Status.ACCEPTED) {
                record.setAnsweredAt(LocalDateTime.now());
            }
            if (status == CallRecord.Status.ENDED || status == CallRecord.Status.DECLINED || status == CallRecord.Status.MISSED) {
                record.setEndedAt(LocalDateTime.now());
            }
            if (durationSeconds != null) record.setDurationSeconds(durationSeconds);
            CallRecord saved = callRecordRepository.save(record);

            // Chat event
            int dur = durationSeconds != null ? durationSeconds : 0;
            pushCallEvent(saved, status.name() + ":" + dur);

            return saved;
        });
    }

    /** Saves a CALL_EVENT message and pushes it to both users via WebSocket. */
    private void pushCallEvent(CallRecord record, String payload) {
        try {
            ChatMessage msg = chatService.saveCallEvent(
                    record.getCaller().getId(),
                    record.getCallee().getId(),
                    payload);
            Object dto = chatService.toDto(msg);
            messaging.convertAndSend("/topic/chat/" + record.getCaller().getId(), dto);
            messaging.convertAndSend("/topic/chat/" + record.getCallee().getId(), dto);
        } catch (Exception ignored) {}
    }

    public List<CallRecord> getPendingForCallee(Long calleeId) {
        return callRecordRepository.findRecentRingingForCallee(calleeId, LocalDateTime.now().minusSeconds(60));
    }

    public List<CallRecord> getHistory(Long userId) {
        return callRecordRepository.findByUserId(userId);
    }

    public Map<String, Object> toDto(CallRecord c, Long viewerId) {
        boolean outgoing = c.getCaller().getId().equals(viewerId);
        User other = outgoing ? c.getCallee() : c.getCaller();
        // HashMap (not Map.of) — tolerates null values for safer serialization.
        Map<String, Object> dto = new HashMap<>();
        dto.put("id",              c.getId());
        dto.put("otherId",         other.getId());
        dto.put("otherName",       other.getName() != null ? other.getName() : "Unknown");
        dto.put("roomId",          c.getRoomId());
        dto.put("status",          c.getStatus().name());
        dto.put("outgoing",        outgoing);
        dto.put("startedAt",       c.getStartedAt() != null ? c.getStartedAt().toString() : "");
        dto.put("durationSeconds", c.getDurationSeconds() != null ? c.getDurationSeconds() : 0);
        return dto;
    }
}
