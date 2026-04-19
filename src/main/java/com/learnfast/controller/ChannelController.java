package com.learnfast.controller;

import com.learnfast.dto.ChannelDto;
import com.learnfast.dto.ChannelMessageDto;
import com.learnfast.model.Channel;
import com.learnfast.model.ChannelMessage;
import com.learnfast.model.User;
import com.learnfast.repository.ChannelMessageRepository;
import com.learnfast.repository.ChannelRepository;
import com.learnfast.service.AuthService;
import com.learnfast.service.ChannelCallService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/channels")
public class ChannelController {

    private final ChannelRepository channelRepo;
    private final ChannelMessageRepository messageRepo;
    private final AuthService authService;
    private final SimpMessagingTemplate messaging;
    private final ChannelCallService callService;

    public ChannelController(ChannelRepository channelRepo,
                             ChannelMessageRepository messageRepo,
                             AuthService authService,
                             SimpMessagingTemplate messaging,
                             ChannelCallService callService) {
        this.channelRepo = channelRepo;
        this.messageRepo = messageRepo;
        this.authService = authService;
        this.messaging = messaging;
        this.callService = callService;
    }

    private Long uid(HttpSession s) { return (Long) s.getAttribute("userId"); }
    private ResponseEntity<?> unauth() {
        return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
    }

    // ── List channels for current user ──────────────────────────────────────
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> list(HttpSession session) {
        Long userId = uid(session);
        if (userId == null) return unauth();
        List<ChannelDto> list = channelRepo.findByMemberOrCreator(userId)
            .stream().map(ChannelDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    // ── Create channel (mentor only) ─────────────────────────────────────────
    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = uid(session);
        if (userId == null) return unauth();
        User me = authService.findById(userId).orElseThrow();
        if (!"mentor".equals(me.getRole().getName())) {
            return ResponseEntity.status(403).body(Map.of("error", "Only mentors can create channels"));
        }
        String name = (String) body.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name required"));
        }

        Channel c = new Channel();
        c.setName(name.trim());
        c.setCreator(me);
        // creator is also a member
        c.getMembers().add(me);
        channelRepo.save(c);
        return ResponseEntity.ok(ChannelDto.from(c));
    }

    // ── Get channel detail ────────────────────────────────────────────────────
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> get(@PathVariable Long id, HttpSession session) {
        Long userId = uid(session);
        if (userId == null) return unauth();
        Channel c = channelRepo.findById(id).orElse(null);
        if (c == null || !isMember(c, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Not a member"));
        }
        return ResponseEntity.ok(ChannelDto.from(c));
    }

    // ── Add member (creator only) ─────────────────────────────────────────────
    @PostMapping("/{id}/members")
    @Transactional
    public ResponseEntity<?> addMember(@PathVariable Long id,
                                       @RequestBody Map<String, Object> body,
                                       HttpSession session) {
        Long userId = uid(session);
        if (userId == null) return unauth();
        Channel c = channelRepo.findById(id).orElse(null);
        if (c == null || !c.getCreator().getId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Not the creator"));
        }
        Object uidObj = body.get("userId");
        if (uidObj == null) return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        Long memberId;
        try { memberId = Long.parseLong(uidObj.toString()); }
        catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid userId"));
        }
        User member = authService.findById(memberId).orElse(null);
        if (member == null) return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        c.getMembers().add(member);
        channelRepo.save(c);
        return ResponseEntity.ok(ChannelDto.from(c));
    }

    // ── Remove member (creator only) ──────────────────────────────────────────
    @DeleteMapping("/{id}/members/{memberId}")
    @Transactional
    public ResponseEntity<?> removeMember(@PathVariable Long id,
                                          @PathVariable Long memberId,
                                          HttpSession session) {
        Long userId = uid(session);
        if (userId == null) return unauth();
        Channel c = channelRepo.findById(id).orElse(null);
        if (c == null || !c.getCreator().getId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Not the creator"));
        }
        c.getMembers().removeIf(u -> u.getId().equals(memberId));
        channelRepo.save(c);
        return ResponseEntity.ok(ChannelDto.from(c));
    }

    // ── Message history ───────────────────────────────────────────────────────
    @GetMapping("/{id}/messages")
    @Transactional(readOnly = true)
    public ResponseEntity<?> history(@PathVariable Long id, HttpSession session) {
        Long userId = uid(session);
        if (userId == null) return unauth();
        Channel c = channelRepo.findById(id).orElse(null);
        if (c == null || !isMember(c, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Not a member"));
        }
        List<ChannelMessageDto> msgs = messageRepo.findByChannelOrderBySentAtAsc(c)
            .stream().map(ChannelMessageDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(msgs);
    }

    // ── Send message ──────────────────────────────────────────────────────────
    @PostMapping("/{id}/messages")
    @Transactional
    public ResponseEntity<?> sendMessage(@PathVariable Long id,
                                         @RequestBody Map<String, Object> body,
                                         HttpSession session) {
        Long userId = uid(session);
        if (userId == null) return unauth();
        Channel c = channelRepo.findById(id).orElse(null);
        if (c == null || !isMember(c, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Not a member"));
        }
        User sender = authService.findById(userId).orElseThrow();
        String text = (String) body.get("message");
        String type = body.get("messageType") instanceof String t ? t : "TEXT";
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Empty message"));
        }

        ChannelMessage msg = new ChannelMessage();
        msg.setChannel(c);
        msg.setSender(sender);
        msg.setMessage(text);
        msg.setMessageType(type);
        msg.setSentAt(LocalDateTime.now());
        messageRepo.save(msg);

        ChannelMessageDto dto = ChannelMessageDto.from(msg);
        messaging.convertAndSend("/topic/channel/" + id, dto);
        return ResponseEntity.ok(dto);
    }

    // ── Group call: get current peers ─────────────────────────────────────────
    @GetMapping("/{id}/call/peers")
    public ResponseEntity<?> getPeers(@PathVariable Long id, HttpSession session) {
        if (uid(session) == null) return unauth();
        return ResponseEntity.ok(callService.getPeers(id));
    }

    // ── Group call: join (register peer) ──────────────────────────────────────
    @PostMapping("/{id}/call/peers")
    @Transactional(readOnly = true)
    public ResponseEntity<?> joinCall(@PathVariable Long id,
                                      @RequestBody Map<String, Object> body,
                                      HttpSession session) {
        Long userId = uid(session);
        if (userId == null) return unauth();
        Channel c = channelRepo.findById(id).orElse(null);
        if (c == null || !isMember(c, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Not a member"));
        }

        String peerId = (String) body.get("peerId");
        callService.joinRoom(id, userId, peerId);

        // Notify others in the channel about the new peer. HashMap (not Map.of)
        // so null fields won't crash serialization.
        Map<String, Object> event = new HashMap<>();
        event.put("type", "PEER_JOINED");
        event.put("userId", userId);
        event.put("peerId", peerId);
        messaging.convertAndSend("/topic/channel/" + id + "/call", event);

        return ResponseEntity.ok(callService.getPeers(id));
    }

    // ── Group call: leave ─────────────────────────────────────────────────────
    @DeleteMapping("/{id}/call/peers")
    public ResponseEntity<?> leaveCall(@PathVariable Long id, HttpSession session) {
        Long userId = uid(session);
        if (userId == null) return unauth();
        callService.leaveRoom(id, userId);

        Map<String, Object> event = new HashMap<>();
        event.put("type", "PEER_LEFT");
        event.put("userId", userId);
        messaging.convertAndSend("/topic/channel/" + id + "/call", event);

        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ── Start group call (notify all members) ────────────────────────────────
    @PostMapping("/{id}/call/start")
    @Transactional(readOnly = true)
    public ResponseEntity<?> startCall(@PathVariable Long id, HttpSession session) {
        Long userId = uid(session);
        if (userId == null) return unauth();
        Channel c = channelRepo.findById(id).orElse(null);
        if (c == null || !isMember(c, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Not a member"));
        }
        User caller = authService.findById(userId).orElseThrow();

        Map<String, Object> signal = new HashMap<>();
        signal.put("type", "GROUP_CALL_STARTED");
        signal.put("channelId", id);
        signal.put("channelName", c.getName());
        signal.put("callerName", caller.getName());
        // Notify all members via their personal call topic (so call-manager.js
        // can display the incoming notification).
        for (User m : c.getMembers()) {
            if (!m.getId().equals(userId)) {
                messaging.convertAndSend("/topic/call/" + m.getId(), signal);
            }
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private boolean isMember(Channel c, Long userId) {
        if (c.getCreator().getId().equals(userId)) return true;
        return c.getMembers().stream().anyMatch(u -> u.getId().equals(userId));
    }
}
