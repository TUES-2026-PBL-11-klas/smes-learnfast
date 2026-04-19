package com.learnfast.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory tracking of which users are currently in which group-call room.
 * channelId -> (userId -> peerId)
 */
@Component
public class ChannelCallService {

    private final Map<Long, Map<Long, String>> rooms = new ConcurrentHashMap<>();

    public void joinRoom(Long channelId, Long userId, String peerId) {
        rooms.computeIfAbsent(channelId, k -> new ConcurrentHashMap<>()).put(userId, peerId);
    }

    public void leaveRoom(Long channelId, Long userId) {
        Map<Long, String> room = rooms.get(channelId);
        if (room != null) {
            room.remove(userId);
            if (room.isEmpty()) rooms.remove(channelId);
        }
    }

    /** Returns list of {userId, peerId} for everyone currently in the room. */
    public List<Map<String, Object>> getPeers(Long channelId) {
        Map<Long, String> room = rooms.getOrDefault(channelId, Map.of());
        List<Map<String, Object>> list = new ArrayList<>();
        room.forEach((uid, pid) -> {
            // Use HashMap (not Map.of) so downstream consumers can tolerate null
            // peerId without an NPE during serialization.
            Map<String, Object> m = new HashMap<>();
            m.put("userId", uid);
            m.put("peerId", pid);
            list.add(m);
        });
        return list;
    }

    public boolean isActive(Long channelId) {
        Map<Long, String> room = rooms.get(channelId);
        return room != null && !room.isEmpty();
    }
}
