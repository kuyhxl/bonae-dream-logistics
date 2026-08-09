package com.bonae.logistics.message.domain.repository;

import com.bonae.logistics.message.domain.entity.MessageAiErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageAiErrorLogRepository extends JpaRepository<MessageAiErrorLog, UUID> {
}
