package com.bonae.logistics.user.domain.repository;

import com.bonae.logistics.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByUsername(String username);

    Optional<User> findByUsernameAndDeletedAtIsNull(String username);
}
