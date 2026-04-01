package com.learnfast.service;

import com.learnfast.model.Role;
import com.learnfast.model.Subject;
import com.learnfast.model.User;
import com.learnfast.repository.RoleRepository;
import com.learnfast.repository.SubjectRepository;
import com.learnfast.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SubjectRepository subjectRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       SubjectRepository subjectRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.subjectRepository = subjectRepository;
    }

    public List<User> getMentors() {
        Role mentorRole = roleRepository.findByName("mentor")
            .orElseThrow(() -> new RuntimeException("Mentor role not found"));
        return userRepository.findByRole(mentorRole);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public User updateSubjects(User user, Set<Long> subjectIds) {
        Set<Subject> subjects = subjectIds.stream()
            .map(id -> subjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject not found: " + id)))
            .collect(Collectors.toSet());
        user.setSubjects(subjects);
        return userRepository.save(user);
    }

    public User updateProfile(User user, String name, String bio, Integer age, String avatarUrl) {
        if (name != null) user.setName(name);
        if (bio != null) user.setBio(bio);
        if (age != null) user.setAge(age);
        if (avatarUrl != null) user.setAvatarUrl(avatarUrl);
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
