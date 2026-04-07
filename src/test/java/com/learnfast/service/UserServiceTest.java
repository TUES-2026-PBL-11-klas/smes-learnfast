package com.learnfast.service;

import com.learnfast.service.UserService;
import com.learnfast.model.Role;
import com.learnfast.model.Subject;
import com.learnfast.model.User;
import com.learnfast.repository.RoleRepository;
import com.learnfast.repository.SubjectRepository;
import com.learnfast.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private UserService userService;

    private Role mentorRole;
    private User mentor1;
    private User mentor2;
    private User student;

    @BeforeEach
    void setUp() {
        mentorRole = new Role();
        mentorRole.setId(1L);
        mentorRole.setName("mentor");

        mentor1 = new User();
        mentor1.setId(1L);
        mentor1.setName("Alice");
        mentor1.setRole(mentorRole);

        mentor2 = new User();
        mentor2.setId(2L);
        mentor2.setName("Bob");
        mentor2.setRole(mentorRole);

        Role studentRole = new Role();
        studentRole.setName("student");

        student = new User();
        student.setId(3L);
        student.setName("Carol");
        student.setRole(studentRole);
        student.setAge(22);
        student.setBio("Learning Java");
        student.setAvatarUrl("https://example.com/carol.png");
    }

    // ─── getMentors ────────────────────────────────────────────────────────────

    @Test
    void getMentors_returnsAllMentors() {
        when(roleRepository.findByName("mentor")).thenReturn(Optional.of(mentorRole));
        when(userRepository.findByRole(mentorRole)).thenReturn(List.of(mentor1, mentor2));

        List<User> result = userService.getMentors();

        assertThat(result).hasSize(2).containsExactlyInAnyOrder(mentor1, mentor2);
    }

    @Test
    void getMentors_returnsEmptyListWhenNoMentors() {
        when(roleRepository.findByName("mentor")).thenReturn(Optional.of(mentorRole));
        when(userRepository.findByRole(mentorRole)).thenReturn(List.of());

        List<User> result = userService.getMentors();

        assertThat(result).isEmpty();
    }

    @Test
    void getMentors_throwsWhenMentorRoleNotFound() {
        when(roleRepository.findByName("mentor")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMentors())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Mentor role not found");
    }

    // ─── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_returnsUserWhenFound() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(student));

        Optional<User> result = userService.findById(3L);

        assertThat(result).isPresent().contains(student);
    }

    @Test
    void findById_returnsEmptyWhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<User> result = userService.findById(99L);

        assertThat(result).isEmpty();
    }

    // ─── updateSubjects ────────────────────────────────────────────────────────

    @Test
    void updateSubjects_setsSubjectsAndSaves() {
        Subject math = new Subject();
        math.setId(10L);
        math.setName("Math");

        Subject science = new Subject();
        science.setId(11L);
        science.setName("Science");

        when(subjectRepository.findById(10L)).thenReturn(Optional.of(math));
        when(subjectRepository.findById(11L)).thenReturn(Optional.of(science));
        when(userRepository.save(student)).thenReturn(student);

        User result = userService.updateSubjects(student, Set.of(10L, 11L));

        assertThat(result.getSubjects()).containsExactlyInAnyOrder(math, science);
        verify(userRepository).save(student);
    }

    @Test
    void updateSubjects_throwsWhenSubjectNotFound() {
        when(subjectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateSubjects(student, Set.of(99L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Subject not found: 99");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateSubjects_setsEmptySetWhenNoIdsProvided() {
        when(userRepository.save(student)).thenReturn(student);

        User result = userService.updateSubjects(student, Set.of());

        assertThat(result.getSubjects()).isEmpty();
        verify(userRepository).save(student);
    }

    // ─── updateProfile ─────────────────────────────────────────────────────────

    @Test
    void updateProfile_updatesAllProvidedFields() {
        when(userRepository.save(student)).thenReturn(student);

        User result = userService.updateProfile(student, "Carol Updated", "New bio", 25,
                "https://example.com/new.png");

        assertThat(result.getName()).isEqualTo("Carol Updated");
        assertThat(result.getBio()).isEqualTo("New bio");
        assertThat(result.getAge()).isEqualTo(25);
        assertThat(result.getAvatarUrl()).isEqualTo("https://example.com/new.png");
        verify(userRepository).save(student);
    }

    @Test
    void updateProfile_skipsNullFields() {
        when(userRepository.save(student)).thenReturn(student);

        // only update bio; name/age/avatar stay as they were
        User result = userService.updateProfile(student, null, "Updated bio", null, null);

        assertThat(result.getName()).isEqualTo("Carol");       // unchanged
        assertThat(result.getBio()).isEqualTo("Updated bio"); // updated
        assertThat(result.getAge()).isEqualTo(22);            // unchanged
        assertThat(result.getAvatarUrl()).isEqualTo("https://example.com/carol.png"); // unchanged
        verify(userRepository).save(student);
    }

    @Test
    void updateProfile_withAllNullsStillSaves() {
        when(userRepository.save(student)).thenReturn(student);

        userService.updateProfile(student, null, null, null, null);

        verify(userRepository).save(student);
    }

    // ─── getAllUsers ────────────────────────────────────────────────────────────

    @Test
    void getAllUsers_returnsAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(mentor1, mentor2, student));

        List<User> result = userService.getAllUsers();

        assertThat(result).hasSize(3).containsExactlyInAnyOrder(mentor1, mentor2, student);
    }

    @Test
    void getAllUsers_returnsEmptyListWhenNoUsers() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<User> result = userService.getAllUsers();

        assertThat(result).isEmpty();
    }

    // ─── deleteUser ────────────────────────────────────────────────────────────

    @Test
    void deleteUser_callsRepositoryDeleteById() {
        doNothing().when(userRepository).deleteById(3L);

        userService.deleteUser(3L);

        verify(userRepository).deleteById(3L);
    }
}
