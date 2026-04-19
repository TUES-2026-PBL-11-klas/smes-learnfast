package com.learnfast.repository;

import com.learnfast.model.User;
import com.learnfast.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByRole(Role role);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByRoleNameAndStatus(String roleName, String status);
}
