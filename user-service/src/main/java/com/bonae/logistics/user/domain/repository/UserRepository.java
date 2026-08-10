package com.bonae.logistics.user.domain.repository;

import com.bonae.logistics.user.domain.entity.Status;
import com.bonae.logistics.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, UserRepositoryCustom {

    boolean existsByUsername(String username);

    Optional<User> findByUsernameAndDeletedAtIsNull(String username);

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndStatusAndDeletedAtIsNull(String username, Status status);
}
