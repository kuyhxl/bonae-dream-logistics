package com.bonae.logistics.message.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.message.infrastructure.client.UserClient;
import com.bonae.logistics.message.infrastructure.client.dto.UserInfoClientResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SlackRecipientResolverTest {

    @Mock
    UserClient userClient;
    @InjectMocks
    SlackRecipientResolver resolver;

    @Test
    @DisplayName("username으로 슬랙 ID를 조회한다")
    void resolvesSlackId() {
        given(userClient.getUserInfo("user01"))
                .willReturn(UserInfoClientResponse.builder().slackId("U12345").build());

        assertThat(resolver.resolveSlackId("user01")).isEqualTo("U12345");
    }

    @Test
    @DisplayName("슬랙 ID가 등록되지 않은 사용자면 예외를 던진다")
    void throwsWhenSlackIdMissing() {
        given(userClient.getUserInfo("user01"))
                .willReturn(UserInfoClientResponse.builder().slackId(null).build());

        assertThatThrownBy(() -> resolver.resolveSlackId("user01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SLACK_ID_NOT_REGISTERED);
    }
}