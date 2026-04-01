package com.learnfast.dto;

import java.util.Set;
import java.util.stream.Collectors;
import com.learnfast.model.User;
import com.learnfast.model.Subject;

public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String role;
    private String name;
    private Integer age;
    private String bio;
    private String avatarUrl;
    private Set<String> subjects;
    private Double averageRating;
    private Integer reviewCount;

    public static UserDto from(User user) {
        UserDto dto = new UserDto();
        dto.id = user.getId();
        dto.username = user.getUsername();
        dto.email = user.getEmail();
        dto.role = user.getRole().getName();
        dto.name = user.getName();
        dto.age = user.getAge();
        dto.bio = user.getBio();
        dto.avatarUrl = user.getAvatarUrl();
        dto.subjects = user.getSubjects().stream()
            .map(Subject::getName).collect(Collectors.toSet());
        
        if ("mentor".equals(user.getRole().getName()) && user.getReviews() != null && !user.getReviews().isEmpty()) {
            dto.reviewCount = user.getReviews().size();
            double sum = user.getReviews().stream().mapToInt(com.learnfast.model.Review::getRating).sum();
            dto.averageRating = Math.round((sum / dto.reviewCount) * 10.0) / 10.0;
        } else {
            dto.reviewCount = 0;
            dto.averageRating = 0.0;
        }
        
        return dto;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getName() { return name; }
    public Integer getAge() { return age; }
    public String getBio() { return bio; }
    public String getAvatarUrl() { return avatarUrl; }
    public Set<String> getSubjects() { return subjects; }
    public Double getAverageRating() { return averageRating; }
    public Integer getReviewCount() { return reviewCount; }
}
