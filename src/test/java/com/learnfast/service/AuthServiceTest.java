package com.learnfast;

import com.learnfast.service.AuthService;
import com.learnfast.model.Role;
import com.learnfast.model.User;
import com.learnfast.repository.RoleRepository;
import com.learnfast.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private AuthService authService;

    private Role studentRole;
    private User existingUser;
    private BCryptPasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();

        studentRole = new Role();
        studentRole.setId(1L);
        studentRole.setName("student");

        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("john");
        existingUser.setEmail("john@example.com");
        existingUser.setPassword(encoder.encode("secret"));
        existingUser.setRole(studentRole);
        existingUser.setName("John Doe");
        existingUser.setAge(25);
        existingUser.setBio("Hello!");

        // Replace the internal encoder with our known instance so we can verify passwords
        ReflectionTestUtils.setField(authService, "passwordEncoder", encoder);
    }

    // ─── register ──────────────────────────────────────────────────────────────

    @Test
    void register_success() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByName("student")).thenReturn(Optional.of(studentRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = authService.register("newuser", "new@example.com", "pass123",
                "student", "New User", 22, "bio");

        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getRole()).isEqualTo(studentRole);
        assertThat(result.getName()).isEqualTo("New User");
        assertThat(result.getAge()).isEqualTo(22);
        assertThat(result.getBio()).isEqualTo("bio");
        // password must be encoded, not stored in plain text
        assertThat(result.getPassword()).isNotEqualTo("pass123");
        assertThat(encoder.matches("pass123", result.getPassword())).isTrue();

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throwsWhenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThatThrownBy(() ->
                authService.register("john", "other@example.com", "pass",
                        "student", "John", 20, "bio"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_throwsWhenEmailAlreadyExists() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() ->
                authService.register("newuser", "john@example.com", "pass",
                        "student", "New", 20, "bio"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_throwsWhenRoleNotFound() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByName("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.register("newuser", "new@example.com", "pass",
                        "ghost", "New", 20, "bio"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid role: ghost");

        verify(userRepository, never()).save(any());
    }

    // ─── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_success() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(existingUser));

        User result = authService.login("john", "secret");

        assertThat(result).isEqualTo(existingUser);
    }

    @Test
    void login_throwsWhenUserNotFound() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("nobody", "pass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void login_throwsWhenPasswordDoesNotMatch() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authService.login("john", "wrongpassword"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid username or password");
    }

    // ─── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_returnsUserWhenFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        Optional<User> result = authService.findById(1L);

        assertThat(result).isPresent().contains(existingUser);
    }

    @Test
    void findById_returnsEmptyWhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<User> result = authService.findById(99L);

        assertThat(result).isEmpty();
    }
}
