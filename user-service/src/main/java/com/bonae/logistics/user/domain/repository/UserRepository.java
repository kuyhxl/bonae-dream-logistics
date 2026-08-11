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

    // 논리 삭제된 사용자는 조회·수정·삭제 대상에서 제외한다.
    Optional<User> findByIdAndDeletedAtIsNull(UUID id);
}
