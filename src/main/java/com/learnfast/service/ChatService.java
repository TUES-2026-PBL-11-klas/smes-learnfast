package com.learnfast.service;

import com.learnfast.dto.ChatMessageDto;
import com.learnfast.model.ChatMessage;
import com.learnfast.model.User;
import com.learnfast.repository.ChatMessageRepository;
import com.learnfast.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
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
        User receiver = userRepository.findById(receiverId)
            .orElseThrow(() -> new RuntimeException("Receiver not found"));

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSender(sender);
        chatMessage.setReceiver(receiver);
        chatMessage.setMessage(message);
        chatMessage.setSentAt(LocalDateTime.now());

        return chatMessageRepository.save(chatMessage);
    }

    public List<ChatMessageDto> getConversation(User currentUser, Long otherUserId) {
        User otherUser = userRepository.findById(otherUserId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        return chatMessageRepository.findConversation(currentUser, otherUser)
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<User> getConversationPartners(User user) {
        return chatMessageRepository.findConversationPartners(user);
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
        dto.setSentAt(msg.getSentAt());
        return dto;
    }
}
