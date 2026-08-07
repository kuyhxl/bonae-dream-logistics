package com.bonae.logistics.message.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SlackMessage extends JpaRepository<SlackMessage, UUID> {
}
