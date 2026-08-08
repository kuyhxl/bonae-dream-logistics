package com.bonae.logistics.message.domain.repository;

import com.bonae.logistics.message.domain.entity.AiDispatchLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiDispatchLogRepository extends JpaRepository<AiDispatchLog, UUID> {
}
