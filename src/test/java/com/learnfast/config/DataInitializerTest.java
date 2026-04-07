package com.learnfast.config;

import com.learnfast.model.Role;
import com.learnfast.model.User;
import com.learnfast.repository.RoleRepository;
import com.learnfast.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock RoleRepository roleRepository;
    @Mock UserRepository userRepository;

    @InjectMocks DataInitializer dataInitializer;

    private Role studentRole;
    private Role mentorRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        studentRole = new Role("student");
        studentRole.setId(1L);

        mentorRole = new Role("mentor");
        mentorRole.setId(2L);

        adminRole = new Role("admin");
        adminRole.setId(3L);
    }

    // ── helper: run the initializer ───────────────────────────────────────────

    private void runInit() throws Exception {
        CommandLineRunner runner = dataInitializer.initData(roleRepository, userRepository);
        runner.run();
    }

    // ── roles ─────────────────────────────────────────────────────────────────

    @Test
    void createsStudentRole_whenItDoesNotExist() throws Exception {
        when(roleRepository.findByName("student")).thenReturn(Optional.empty());
        when(roleRepository.findByName("mentor")).thenReturn(Optional.of(mentorRole));
        when(roleRepository.findByName("admin")).thenReturn(Optional.of(adminRole));
        when(userRepository.findByUsername(any())).thenReturn(Optional.of(new User()));

        runInit();

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Role::getName)
                .contains("student");
    }

    @Test
    void createsMentorRole_whenItDoesNotExist() throws Exception {
        when(roleRepository.findByName("student")).thenReturn(Optional.of(studentRole));
        when(roleRepository.findByName("mentor")).thenReturn(Optional.empty());
        when(roleRepository.findByName("admin")).thenReturn(Optional.of(adminRole));
        when(userRepository.findByUsername(any())).thenReturn(Optional.of(new User()));

        runInit();

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Role::getName)
                .contains("mentor");
    }

    @Test
    void createsAdminRole_whenItDoesNotExist() throws Exception {
        when(roleRepository.findByName("student")).thenReturn(Optional.of(studentRole));
        when(roleRepository.findByName("mentor")).thenReturn(Optional.of(mentorRole));
        when(roleRepository.findByName("admin")).thenReturn(Optional.empty());
        // admin role save returns the saved role so the user creation can look it up
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByUsername(any())).thenReturn(Optional.of(new User()));

        runInit();

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Role::getName)
                .contains("admin");
    }

    @Test
    void doesNotSaveRoles_whenAllAlreadyExist() throws Exception {
        when(roleRepository.findByName("student")).thenReturn(Optional.of(studentRole));
        when(roleRepository.findByName("mentor")).thenReturn(Optional.of(mentorRole));
        when(roleRepository.findByName("admin")).thenReturn(Optional.of(adminRole));
        when(userRepository.findByUsername(any())).thenReturn(Optional.of(new User()));

        runInit();

        verify(roleRepository, never()).save(any(Role.class));
    }

    // ── admin user ────────────────────────────────────────────────────────────

    @Test
    void createsAdminUser_whenNotExists() throws Exception {
        when(roleRepository.findByName("student")).thenReturn(Optional.of(studentRole));
        when(roleRepository.findByName("mentor")).thenReturn(Optional.of(mentorRole));
        when(roleRepository.findByName("admin")).thenReturn(Optional.of(adminRole));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("mentor")).thenReturn(Optional.of(new User()));
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(new User()));

        runInit();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, atLeastOnce()).save(captor.capture());

        User savedAdmin = captor.getAllValues().stream()
                .filter(u -> "admin".equals(u.getUsername()))
                .findFirst()
                .orElseThrow();

        assertThat(savedAdmin.getUsername()).isEqualTo("admin");
        assertThat(savedAdmin.getEmail()).isEqualTo("admin@learnfast.com");
        assertThat(savedAdmin.getName()).isEqualTo("Administrator");
        assertThat(savedAdmin.getAge()).isEqualTo(25);
        assertThat(savedAdmin.getRole()).isEqualTo(adminRole);
        // password must be bcrypt-encoded, not plain text
        assertThat(savedAdmin.getPassword()).isNotEqualTo("admin123");
        assertThat(new BCryptPasswordEncoder().matches("admin123", savedAdmin.getPassword())).isTrue();
    }

    @Test
    void doesNotCreateAdminUser_whenAlreadyExists() throws Exception {
        when(roleRepository.findByName("student")).thenReturn(Optional.of(studentRole));
        when(roleRepository.findByName("mentor")).thenReturn(Optional.of(mentorRole));
        when(roleRepository.findByName("admin")).thenReturn(Optional.of(adminRole));
        when(userRepository.findByUsername(any())).thenReturn(Optional.of(new User()));

        runInit();

        verify(userRepository, never()).save(any(User.class));
    }

    // ── test mentor user ──────────────────────────────────────────────────────

    @Test
    void createsTestMentor_whenNotExists() throws Exception {
        when(roleRepository.findByName("student")).thenReturn(Optional.of(studentRole));
        when(roleRepository.findByName("mentor")).thenReturn(Optional.of(mentorRole));
        when(roleRepository.findByName("admin")).thenReturn(Optional.of(adminRole));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(new User()));
        when(userRepository.findByUsername("mentor")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(new User()));

        runInit();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, atLeastOnce()).save(captor.capture());

        User savedMentor = captor.getAllValues().stream()
                .filter(u -> "mentor".equals(u.getUsername()))
                .findFirst()
                .orElseThrow();

        assertThat(savedMentor.getUsername()).isEqualTo("mentor");
        assertThat(savedMentor.getEmail()).isEqualTo("mentor@learnfast.com");
        assertThat(savedMentor.getName()).isEqualTo("Ivan Petrov");
        assertThat(savedMentor.getAge()).isEqualTo(30);
        assertThat(savedMentor.getRole()).isEqualTo(mentorRole);
        assertThat(savedMentor.getAvatarUrl()).isEqualTo("https://i.pravatar.cc/150?u=mentor");
        assertThat(new BCryptPasswordEncoder().matches("password123", savedMentor.getPassword())).isTrue();
    }

    // ── test student user ─────────────────────────────────────────────────────

    @Test
    void createsTestStudent_whenNotExists() throws Exception {
        when(roleRepository.findByName("student")).thenReturn(Optional.of(studentRole));
        when(roleRepository.findByName("mentor")).thenReturn(Optional.of(mentorRole));
        when(roleRepository.findByName("admin")).thenReturn(Optional.of(adminRole));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(new User()));
        when(userRepository.findByUsername("mentor")).thenReturn(Optional.of(new User()));
        when(userRepository.findByUsername("student")).thenReturn(Optional.empty());

        runInit();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, atLeastOnce()).save(captor.capture());

        User savedStudent = captor.getAllValues().stream()
                .filter(u -> "student".equals(u.getUsername()))
                .findFirst()
                .orElseThrow();

        assertThat(savedStudent.getUsername()).isEqualTo("student");
        assertThat(savedStudent.getEmail()).isEqualTo("student@learnfast.com");
        assertThat(savedStudent.getName()).isEqualTo("Georgi Ivanov");
        assertThat(savedStudent.getAge()).isEqualTo(20);
        assertThat(savedStudent.getRole()).isEqualTo(studentRole);
        assertThat(savedStudent.getAvatarUrl()).isEqualTo("https://i.pravatar.cc/150?u=student");
        assertThat(new BCryptPasswordEncoder().matches("password123", savedStudent.getPassword())).isTrue();
    }

    // ── all fresh (nothing exists) ────────────────────────────────────────────

    @Test
    void freshDatabase_createsAllRolesAndAllUsers() throws Exception {
        when(roleRepository.findByName("student")).thenReturn(Optional.empty());
        when(roleRepository.findByName("mentor")).thenReturn(Optional.empty());
        when(roleRepository.findByName("admin")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> {
            Role r = inv.getArgument(0);
            if ("student".equals(r.getName())) return studentRole;
            if ("mentor".equals(r.getName())) return mentorRole;
            return adminRole;
        });
        // After save, findByName must return the saved role so user creation works
        when(roleRepository.findByName("admin"))
                .thenReturn(Optional.empty())          // first call → triggers save
                .thenReturn(Optional.of(adminRole));   // second call → user lookup
        when(roleRepository.findByName("mentor"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(mentorRole));
        when(roleRepository.findByName("student"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(studentRole));

        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("mentor")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("student")).thenReturn(Optional.empty());

        runInit();

        verify(roleRepository, times(3)).save(any(Role.class));
        verify(userRepository, times(3)).save(any(User.class));
    }
}
