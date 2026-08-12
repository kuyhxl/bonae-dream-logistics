package com.bonae.logistics.message.infrastructure.persistence;

import com.bonae.logistics.message.domain.entity.*;
import com.bonae.logistics.message.domain.repository.SlackMessageRepositoryCustom;
import com.bonae.logistics.message.presentation.dto.request.SlackMessageSearchCondition;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SlackMessageRepositoryImpl implements SlackMessageRepositoryCustom {

    private static final QSlackMessage slackMessage = QSlackMessage.slackMessage;
    private static final String UPDATED_AT = "updatedAt";

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<SlackMessage> search(SlackMessageSearchCondition condition, Pageable pageable) {
        List<SlackMessage> content = queryFactory
                .selectFrom(slackMessage)
                .where(conditions(condition))
                .orderBy(toOrderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 마지막 페이지 등 count가 불필요한 경우 쿼리를 생략한다.
        JPAQuery<Long> countQuery = queryFactory
                .select(slackMessage.count())
                .from(slackMessage)
                .where(conditions(condition));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /* QueryDSL 동적 조건. null을 반환하면 where 절에서 자동으로 무시된다. */

    private BooleanExpression[] conditions(SlackMessageSearchCondition condition) {
        return new BooleanExpression[]{
                slackMessage.deletedAt.isNull(),
                receiverEq(condition.receiverSlackId()),
                sendStatusEq(condition.sendStatus()),
                sourceTypeEq(condition.sourceType()),
                senderEq(condition.senderUsername()),
                messageContains(condition.keyword())
        };
    }

    private BooleanExpression receiverEq(String receiverSlackId) {
        return StringUtils.hasText(receiverSlackId) ? slackMessage.receiverSlackId.eq(receiverSlackId) : null;
    }

    private BooleanExpression sendStatusEq(SendStatus sendStatus) {
        return sendStatus == null ? null : slackMessage.sendStatus.eq(sendStatus);
    }

    private BooleanExpression sourceTypeEq(SourceType sourceType) {
        return sourceType == null ? null : slackMessage.sourceType.eq(sourceType);
    }

    private BooleanExpression messageContains(String keyword) {
        return StringUtils.hasText(keyword) ? slackMessage.message.contains(keyword) : null;
    }

    // 감사 추적: 발신자는 BaseEntity의 created_by에 기록된다.
    private BooleanExpression senderEq(String senderUsername) {
        return StringUtils.hasText(senderUsername) ? slackMessage.createdBy.eq(senderUsername) : null;
    }

    /* sort는 PageRequestDto가 createdAt/updatedAt으로 이미 검증했다. */
    private OrderSpecifier<?>[] toOrderSpecifiers(Pageable pageable) {
        return pageable.getSort().stream()
                .map(order -> new OrderSpecifier<>(
                        order.isAscending() ? Order.ASC : Order.DESC,
                        UPDATED_AT.equals(order.getProperty()) ? slackMessage.updatedAt : slackMessage.createdAt))
                .toArray(OrderSpecifier[]::new);
    }
}