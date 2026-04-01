package com.learnfast.config;

import com.learnfast.model.Role;
import com.learnfast.model.User;
import com.learnfast.repository.RoleRepository;
import com.learnfast.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(RoleRepository roleRepository, UserRepository userRepository) {
        return args -> {
            // Create roles if they don't exist
            if (roleRepository.findByName("student").isEmpty()) {
                roleRepository.save(new Role("student"));
            }
            if (roleRepository.findByName("mentor").isEmpty()) {
                roleRepository.save(new Role("mentor"));
            }
            if (roleRepository.findByName("admin").isEmpty()) {
                roleRepository.save(new Role("admin"));
            }

            // Create default admin if not exists
            if (userRepository.findByUsername("admin").isEmpty()) {
                Role adminRole = roleRepository.findByName("admin").get();
                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@learnfast.com");
                admin.setPassword(new BCryptPasswordEncoder().encode("admin123"));
                admin.setRole(adminRole);
                admin.setName("Administrator");
                admin.setAge(25);
                admin.setBio("Platform administrator");
                userRepository.save(admin);
            }

            // Create test mentor if not exists
            if (userRepository.findByUsername("mentor").isEmpty()) {
                Role mentorRole = roleRepository.findByName("mentor").get();
                User mentor = new User();
                mentor.setUsername("mentor");
                mentor.setEmail("mentor@learnfast.com");
                mentor.setPassword(new BCryptPasswordEncoder().encode("password123"));
                mentor.setRole(mentorRole);
                mentor.setName("Ivan Petrov");
                mentor.setAge(30);
                mentor.setBio("Expert Math and Physics mentor with 5 years of experience.");
                mentor.setAvatarUrl("https://i.pravatar.cc/150?u=mentor");
                userRepository.save(mentor);
            }

            // Create test student if not exists
            if (userRepository.findByUsername("student").isEmpty()) {
                Role studentRole = roleRepository.findByName("student").get();
                User student = new User();
                student.setUsername("student");
                student.setEmail("student@learnfast.com");
                student.setPassword(new BCryptPasswordEncoder().encode("password123"));
                student.setRole(studentRole);
                student.setName("Georgi Ivanov");
                student.setAge(20);
                student.setBio("Enthusiastic student looking to improve my coding skills.");
                student.setAvatarUrl("https://i.pravatar.cc/150?u=student");
                userRepository.save(student);
            }
        };
    }
}
