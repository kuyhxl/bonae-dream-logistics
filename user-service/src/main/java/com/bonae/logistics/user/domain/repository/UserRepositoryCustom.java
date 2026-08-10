package com.bonae.logistics.user.domain.repository;

import com.bonae.logistics.user.domain.entity.DeliveryManagerType;
import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.domain.entity.Status;
import com.bonae.logistics.user.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface UserRepositoryCustom {

    List<User> searchDeliveryManagers(UUID hubId, DeliveryManagerType type);


    // 도메인 계층이 presentation DTO에 의존하지 않도록 조건을 개별 파라미터로 받는다.
    Page<User> searchUsers(String keyword, Role role, Status status, UUID hubId, Pageable pageable);
}
