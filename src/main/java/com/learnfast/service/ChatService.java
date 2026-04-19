package com.learnfast.service;

import com.learnfast.dto.ChatMessageDto;
import com.learnfast.exception.ResourceNotFoundException;
import com.learnfast.model.ChatMessage;
import com.learnfast.model.User;
import com.learnfast.repository.ChatMessageRepository;
import com.learnfast.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public ChatService(ChatMessageRepository chatMessageRepository, UserRepository userRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

    public ChatMessage saveMessage(User sender, Long receiverId, String message) {
        return saveMessage(sender, receiverId, message, "TEXT");
    }

    public ChatMessage saveMessage(User sender, Long receiverId, String message, String messageType) {
        User receiver = userRepository.findById(receiverId)
            .orElseThrow(() -> new ResourceNotFoundException("Receiver", receiverId));

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSender(sender);
        chatMessage.setReceiver(receiver);
        chatMessage.setMessage(message);
        chatMessage.setMessageType(messageType != null ? messageType : "TEXT");
        chatMessage.setSentAt(LocalDateTime.now());

        return chatMessageRepository.save(chatMessage);
    }

    public List<ChatMessageDto> getConversation(User currentUser, Long otherUserId) {
        User otherUser = userRepository.findById(otherUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User", otherUserId));

        return chatMessageRepository.findConversation(currentUser, otherUser)
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<User> getConversationPartners(User user) {
        Set<User> partners = new HashSet<>();
        partners.addAll(chatMessageRepository.findPartnersBySender(user));
        partners.addAll(chatMessageRepository.findPartnersByReceiver(user));
        return new ArrayList<>(partners);
    }

    public ChatMessageDto toDto(ChatMessage msg) {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setId(msg.getId());
        dto.setSenderId(msg.getSender().getId());
        dto.setSenderName(msg.getSender().getName());
        dto.setSenderUsername(msg.getSender().getUsername());
        dto.setReceiverId(msg.getReceiver().getId());
        dto.setReceiverName(msg.getReceiver().getName());
        dto.setReceiverUsername(msg.getReceiver().getUsername());
        dto.setMessage(msg.getMessage());
        dto.setMessageType(msg.getMessageType());
        dto.setSentAt(msg.getSentAt());
        return dto;
    }

    /** Save a CALL_EVENT system message between two users. */
    public ChatMessage saveCallEvent(Long callerId, Long calleeId, String eventPayload) {
        User caller = userRepository.findById(callerId).orElseThrow();
        User callee = userRepository.findById(calleeId).orElseThrow();
        ChatMessage msg = new ChatMessage();
        msg.setSender(caller);
        msg.setReceiver(callee);
        msg.setMessage(eventPayload);
        msg.setMessageType("CALL_EVENT");
        msg.setSentAt(LocalDateTime.now());
        return chatMessageRepository.save(msg);
    }
}
