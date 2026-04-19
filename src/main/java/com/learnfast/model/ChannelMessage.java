package com.learnfast.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "channel_messages")
public class ChannelMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "message_type", nullable = false, length = 20)
    private String messageType = "TEXT";

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }
    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String t) { this.messageType = t; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime t) { this.sentAt = t; }
}
