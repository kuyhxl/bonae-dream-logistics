package com.bonae.logistics.message.application.service;

/*
 * 마스터 관리자의 슬랙 메시지 조회·수정·삭제를 담당한다.
 * 발송 흐름(SlackMessageService)과 트랜잭션 경계가 달라 별도 서비스로 분리했다.
 */

import com.bonae.logistics.common.auth.CurrentAuditorProvider;
import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.message.domain.entity.SlackMessage;
import com.bonae.logistics.message.domain.repository.SlackMessageRepository;
import com.bonae.logistics.message.presentation.dto.request.SlackMessageSearchCondition;
import com.bonae.logistics.message.presentation.dto.request.SlackMessageUpdateRequest;
import com.bonae.logistics.message.presentation.dto.response.SlackMessageDetailResponse;
import com.bonae.logistics.message.presentation.dto.response.SlackMessageListItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlackMessageAdminService {

    private final SlackMessageRepository slackMessageRepository;
    private final CurrentAuditorProvider currentAuditorProvider;

    public PageResponseDto<SlackMessageListItemResponse> search(SlackMessageSearchCondition condition,
                                                                PageRequestDto pageRequestDto) {
        Page<SlackMessage> page = slackMessageRepository.search(condition, pageRequestDto.toPageable());
        return PageResponseDto.from(page, SlackMessageListItemResponse::from);
    }

    public SlackMessageDetailResponse getDetail(UUID slackMessageId) {
        return SlackMessageDetailResponse.from(findActive(slackMessageId));
    }

    private SlackMessage findActive(UUID slackMessageId) {
        return slackMessageRepository.findByIdAndDeletedAtIsNull(slackMessageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SLACK_MESSAGE_NOT_FOUND));
    }

    @Transactional
    public SlackMessageDetailResponse update(UUID slackMessageId, SlackMessageUpdateRequest request) {
        SlackMessage slackMessage = findActive(slackMessageId);
        slackMessage.update(request.receiverSlackId(), request.message());
        return SlackMessageDetailResponse.from(slackMessage);
    }

    @Transactional
    public void delete(UUID slackMessageId) {
        // deleted_at, deleted_by(= X-User-Id)를 기록한다. 물리 삭제하지 않는다.
        findActive(slackMessageId).delete(currentAuditorProvider.getCurrentAuditorOrSystem());
    }
}