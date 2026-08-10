package com.bonae.logistics.message.domain.repository;

import com.bonae.logistics.message.domain.entity.SlackMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SlackMessageRepository extends JpaRepository<SlackMessage, UUID>, SlackMessageRepositoryCustom {

    /* 논리 삭제된 메시지는 없는 것으로 취급한다(공통 규격: 404). */
    Optional<SlackMessage> findByIdAndDeletedAtIsNull(UUID id);
}