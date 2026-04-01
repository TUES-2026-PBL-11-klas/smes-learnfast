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
        };
    }
}
