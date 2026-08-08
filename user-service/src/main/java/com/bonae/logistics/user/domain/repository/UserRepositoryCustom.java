package com.bonae.logistics.user.domain.repository;

import com.bonae.logistics.user.domain.entity.DeliveryManagerType;
import com.bonae.logistics.user.domain.entity.User;

import java.util.List;
import java.util.UUID;

public interface UserRepositoryCustom {

    List<User> searchDeliveryManagers(UUID hubId, DeliveryManagerType type);
}
