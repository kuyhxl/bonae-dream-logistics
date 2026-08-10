package com.bonae.logistics.user.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.user.domain.entity.DeliveryManagerType;
import com.bonae.logistics.user.domain.entity.Status;
import com.bonae.logistics.user.domain.entity.User;
import com.bonae.logistics.user.domain.repository.UserRepository;
import com.bonae.logistics.user.presentation.dto.request.UserSearchCondition;
import com.bonae.logistics.user.presentation.dto.response.DeliveryManagerResponse;
import com.bonae.logistics.user.presentation.dto.response.UserInfoResponse;
import com.bonae.logistics.user.presentation.dto.response.UserSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    // 배송 배정 후보 조회. 결과가 비어도 예외로 보지 않고 빈 목록을 반환한다.
    // 배정 가능한 담당자가 없다는 판단(DELIVERY_MANAGER_NOT_AVAILABLE)은 호출 측인 delivery-service의 책임이다.
    public List<DeliveryManagerResponse> getDeliveryManagers(UUID hubId, DeliveryManagerType type) {
        return userRepository.searchDeliveryManagers(hubId, type)
                .stream()
                .map(DeliveryManagerResponse::new)
                .toList();
    }

    public UserInfoResponse getUserInfo(String username) {
        User user = userRepository
                .findByUsernameAndStatusAndDeletedAtIsNull(username, Status.APPROVED)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return new UserInfoResponse(user);
    }

    // 사용자 목록·검색. 조건은 전부 선택이며, 생략된 조건은 필터에서 제외된다.
    public PageResponseDto<UserSummaryResponse> searchUsers(UserSearchCondition condition,
                                                            PageRequestDto pageRequestDto) {
        Page<User> users = userRepository.searchUsers(
                condition.getKeyword(),
                condition.toRole(),
                condition.toStatus(),
                condition.toHubId(),
                pageRequestDto.toPageable()
        );

        return PageResponseDto.from(users, UserSummaryResponse::from);
    }
}
