package com.bonae.logistics.order.domain.repository;

import com.bonae.logistics.order.domain.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}
