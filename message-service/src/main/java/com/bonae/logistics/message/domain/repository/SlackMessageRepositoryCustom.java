package com.bonae.logistics.message.domain.repository;

import com.bonae.logistics.message.domain.entity.SlackMessage;
import com.bonae.logistics.message.presentation.dto.request.SlackMessageSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SlackMessageRepositoryCustom {
    Page<SlackMessage> search(SlackMessageSearchCondition condition, Pageable pageable);
}