package com.bonae.logistics.message.domain.repository;

import com.bonae.logistics.message.domain.entity.SlackMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SlackMessageRepository extends JpaRepository<SlackMessage, UUID> {
}
