package com.nila.chores.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findByRoleOrderByDisplayNameAsc(User.Role role);

    /** Used by the at-risk reminder scheduler to load all KID users for processing. */
    List<User> findAllByRole(User.Role role);
}
