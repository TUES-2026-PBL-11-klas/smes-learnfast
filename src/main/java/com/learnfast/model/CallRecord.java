package com.learnfast.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "call_records")
public class CallRecord {

    public enum Status { RINGING, ACCEPTED, DECLINED, MISSED, ENDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "caller_id", nullable = false)
    private User caller;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "callee_id", nullable = false)
    private User callee;

    @Column(name = "room_id", nullable = false, length = 100)
    private String roomId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.RINGING;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    public CallRecord() {}

    public Long getId() { return id; }
    public User getCaller() { return caller; }
    public void setCaller(User caller) { this.caller = caller; }
    public User getCallee() { return callee; }
    public void setCallee(User callee) { this.callee = callee; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(LocalDateTime answeredAt) { this.answeredAt = answeredAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
}
