package com.bonae.logistics.user.infrastructure.persistence;

import com.bonae.logistics.user.domain.entity.*;
import com.bonae.logistics.user.domain.repository.UserRepositoryCustom;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private static final QUser user = QUser.user;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<User> searchDeliveryManagers(UUID hubId, DeliveryManagerType type) {
        return queryFactory
                .selectFrom(user)
                .where(
                        user.role.eq(Role.DELIVERY_MANAGER),
                        user.status.eq(Status.APPROVED),
                        user.deletedAt.isNull(),
                        hubIdEq(hubId),
                        managerTypeMatches(type)
                )
                // 배정 순번은 delivery-service가 관리하므로 여기선 가입순으로 안정 정렬만 보장한다.
                .orderBy(user.createdAt.asc())
                .fetch(); // List로 반환.
    }

    /* QueryDSL 동적 조건 메서드 */

    // null 반환 시 where 절에서 자동으로 무시된다. -> null이 아니면 hubId가 일치하는 조건을 반환한다.
    private BooleanExpression hubIdEq(UUID hubId) {
        return hubId == null ? null : user.hubId.eq(hubId);
    }

    private BooleanExpression managerTypeMatches(DeliveryManagerType type) {
        if (type == null) {
            return null;
        }
        // type으로 HUB_DELIVERY가 들어오면 hubId가 null인 조건을 반환한다.
        return type == DeliveryManagerType.HUB_DELIVERY
                ? user.hubId.isNull()
                : user.hubId.isNotNull();
    }

    @Override
    public Page<User> searchUsers(String keyword, Role role, Status status, UUID hubId, Pageable pageable) {
        // 논리 삭제된 사용자는 조회·검색에서 제외한다.
        BooleanExpression[] conditions = {
                user.deletedAt.isNull(),
                keywordContains(keyword),
                roleEq(role),
                statusEq(status),
                hubIdEq(hubId)
        };

        List<User> content = queryFactory
                .selectFrom(user)
                .where(conditions)
                .orderBy(toOrderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(user.count())
                .from(user)
                .where(conditions);

        // 마지막 페이지이거나 첫 페이지가 다 안 찬 경우 count 쿼리를 생략한다.
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // 아이디·이름·슬랙ID·소속명을 대소문자 구분 없이 부분 일치로 검색한다.
    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return user.username.containsIgnoreCase(keyword)
                .or(user.name.containsIgnoreCase(keyword))
                .or(user.slackId.containsIgnoreCase(keyword))
                .or(user.affiliationName.containsIgnoreCase(keyword));
    }

    private BooleanExpression roleEq(Role role) {
        return role == null ? null : user.role.eq(role);
    }

    private BooleanExpression statusEq(Status status) {
        return status == null ? null : user.status.eq(status);
    }

    // PageRequestDto에서 createdAt/updatedAt만 통과하므로 그 외는 createdAt으로 처리한다.
    private OrderSpecifier<?>[] toOrderSpecifiers(Pageable pageable) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            orders.add(new OrderSpecifier<>(direction,
                    "updatedAt".equals(order.getProperty()) ? user.updatedAt : user.createdAt));
        }
        // updatedAt이 null이거나 값이 같을 때 페이지 간 순서가 흔들리지 않도록 2차 정렬을 붙인다.
        orders.add(user.id.asc());
        return orders.toArray(OrderSpecifier[]::new);
    }
}
