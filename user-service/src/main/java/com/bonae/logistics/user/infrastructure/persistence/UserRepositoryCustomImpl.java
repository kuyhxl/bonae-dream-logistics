package com.bonae.logistics.user.infrastructure.persistence;

import com.bonae.logistics.user.domain.entity.*;
import com.bonae.logistics.user.domain.repository.UserRepositoryCustom;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

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
                .orderBy(user.deletedAt.asc())
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
        // type으로 HUB_DELIVERY가 들어오면 hubId가 null이 아닌 조건을 반환한다.
        return type == DeliveryManagerType.HUB_DELIVERY
                ? user.hubId.isNull()
                : user.hubId.isNotNull();
    }
}
