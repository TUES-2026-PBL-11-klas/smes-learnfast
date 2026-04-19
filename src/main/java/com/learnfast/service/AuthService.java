package com.learnfast.service;

import com.learnfast.exception.BadRequestException;
import com.learnfast.model.Role;
import com.learnfast.model.User;
import com.learnfast.repository.RoleRepository;
import com.learnfast.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public User register(String username, String email, String password,
                         String roleName, String name, Integer age, String bio,
                         String diplomaInfo, Integer yearsOfExperience,
                         String fieldOfExpertise, String motivationToTeach) {
        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already exists");
        }

        Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new RuntimeException("Invalid role: " + roleName));

        if ("mentor".equals(roleName)) {
            if (diplomaInfo == null || diplomaInfo.isBlank()) {
                throw new BadRequestException("Diploma information is required for mentors");
            }
            if (yearsOfExperience == null || yearsOfExperience < 0) {
                throw new BadRequestException("Years of experience is required for mentors");
            }
            if (yearsOfExperience > 0 && (fieldOfExpertise == null || fieldOfExpertise.isBlank())) {
                throw new BadRequestException("Field of expertise is required when you have teaching experience");
            }
            if (yearsOfExperience == 0 && (motivationToTeach == null || motivationToTeach.isBlank())) {
                throw new BadRequestException("Please tell us why you want to start teaching at LearnFast");
            }
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setName(name);
        user.setAge(age);
        user.setBio(bio);

        if ("mentor".equals(roleName)) {
            user.setDiplomaInfo(diplomaInfo);
            user.setYearsOfExperience(yearsOfExperience);
            user.setFieldOfExpertise(fieldOfExpertise);
            user.setMotivationToTeach(motivationToTeach);
            user.setStatus("PENDING_APPROVAL");
        }

        return userRepository.save(user);
    }

    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BadRequestException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("Invalid username or password");
        }

        if ("PENDING_APPROVAL".equals(user.getStatus())) {
            throw new BadRequestException("Your mentor account is awaiting admin approval");
        }
        if ("REJECTED".equals(user.getStatus())) {
            throw new BadRequestException("Your mentor application was rejected by an administrator");
        }

        return user;
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}
