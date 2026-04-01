package com.learnfast.dto;

import java.time.LocalDateTime;

public class SessionDto {
    private Long id;
    private UserDto student;
    private UserDto mentor;
    private String status;
    private LocalDateTime createdAt;
    private String roomId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserDto getStudent() { return student; }
    public void setStudent(UserDto student) { this.student = student; }
    public UserDto getMentor() { return mentor; }
    public void setMentor(UserDto mentor) { this.mentor = mentor; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
}
