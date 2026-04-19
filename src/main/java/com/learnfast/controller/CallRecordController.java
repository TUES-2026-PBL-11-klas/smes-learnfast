package com.learnfast.controller;

import com.learnfast.model.CallRecord;
import com.learnfast.service.CallRecordService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/calls")
public class CallRecordController {

    private final CallRecordService callRecordService;
    private final SimpMessagingTemplate messaging;

    public CallRecordController(CallRecordService callRecordService, SimpMessagingTemplate messaging) {
        this.callRecordService = callRecordService;
        this.messaging = messaging;
    }

    /** Caller creates a record and immediately pushes INVITE to callee via WebSocket */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));

        Long calleeId = Long.parseLong(body.get("calleeId").toString());
        String roomId = body.get("roomId").toString();

        CallRecord record = callRecordService.createCall(userId, calleeId, roomId);

        // Push INVITE directly to callee via server-side WebSocket (reliable — no client STOMP race).
        // Use a mutable HashMap (not Map.of) so a null caller name doesn't throw NPE.
        Map<String, Object> signal = new HashMap<>();
        signal.put("type",       "INVITE");
        signal.put("callerId",   record.getCaller().getId());
        signal.put("callerName", record.getCaller().getName() != null ? record.getCaller().getName() : "Someone");
        signal.put("roomId",     roomId);
        signal.put("callId",     record.getId());
        messaging.convertAndSend("/topic/call/" + calleeId, signal);

        return ResponseEntity.ok(Map.of("callId", record.getId()));
    }

    /** Update status: ACCEPTED, DECLINED, MISSED, ENDED */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @RequestBody Map<String, Object> body,
                                          HttpSession session) {
        if (session.getAttribute("userId") == null)
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));

        CallRecord.Status status = CallRecord.Status.valueOf(body.get("status").toString());
        Integer duration = body.containsKey("durationSeconds")
                ? Integer.parseInt(body.get("durationSeconds").toString()) : null;

        return callRecordService.updateStatus(id, status, duration)
                .map(r -> ResponseEntity.ok(Map.of("ok", true)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** sendBeacon endpoint — body is plain JSON text, no session check needed for fire-and-forget */
    @PostMapping("/{id}/status-beacon")
    public ResponseEntity<?> beacon(@PathVariable Long id, @RequestBody String body) {
        try {
            String status = body.contains("ENDED") ? "ENDED" : "MISSED";
            int dur = 0;
            if (body.contains("durationSeconds")) {
                String s = body.replaceAll(".*\"durationSeconds\"\\s*:\\s*(\\d+).*", "$1");
                try { dur = Integer.parseInt(s.trim()); } catch (Exception ignored) {}
            }
            callRecordService.updateStatus(id, CallRecord.Status.valueOf(status), dur);
        } catch (Exception ignored) {}
        return ResponseEntity.ok().build();
    }

    /** Pending RINGING calls for the current user (as callee) — used for missed-notification recovery */
    @GetMapping("/pending")
    public ResponseEntity<?> pending(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));

        return ResponseEntity.ok(
            callRecordService.getPendingForCallee(userId).stream()
                .map(c -> callRecordService.toDto(c, userId))
                .collect(Collectors.toList())
        );
    }

    /** Get call history for the current user */
    @GetMapping
    public ResponseEntity<?> history(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));

        List<Map<String, Object>> records = callRecordService.getHistory(userId)
                .stream()
                .map(c -> callRecordService.toDto(c, userId))
                .collect(Collectors.toList());

        return ResponseEntity.ok(records);
    }
}
