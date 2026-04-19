package com.learnfast.dto;

import com.learnfast.model.ChannelMessage;
import java.time.LocalDateTime;

public class ChannelMessageDto {
    private Long id;
    private Long channelId;
    private Long senderId;
    private String senderName;
    private String senderUsername;
    private String senderAvatarUrl;
    private String message;
    private String messageType;
    private LocalDateTime sentAt;

    public static ChannelMessageDto from(ChannelMessage m) {
        ChannelMessageDto d = new ChannelMessageDto();
        d.id = m.getId();
        d.channelId = m.getChannel().getId();
        d.senderId = m.getSender().getId();
        d.senderName = m.getSender().getName();
        d.senderUsername = m.getSender().getUsername();
        d.senderAvatarUrl = m.getSender().getAvatarUrl();
        d.message = m.getMessage();
        d.messageType = m.getMessageType();
        d.sentAt = m.getSentAt();
        return d;
    }

    public Long getId() { return id; }
    public Long getChannelId() { return channelId; }
    public Long getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getSenderUsername() { return senderUsername; }
    public String getSenderAvatarUrl() { return senderAvatarUrl; }
    public String getMessage() { return message; }
    public String getMessageType() { return messageType; }
    public LocalDateTime getSentAt() { return sentAt; }
}
