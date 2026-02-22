package com.library.library_management.repository;

import com.library.library_management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    //here t
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

}
