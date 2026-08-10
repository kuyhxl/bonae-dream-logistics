package com.bonae.logistics.user.application.service;

import com.bonae.logistics.common.auth.CurrentAuditorProvider;
import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.user.domain.entity.DeliveryManagerType;
import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.domain.entity.Status;
import com.bonae.logistics.user.domain.entity.User;
import com.bonae.logistics.user.domain.repository.UserRepository;
import com.bonae.logistics.user.presentation.dto.request.SignupRequestSearchCondition;
import com.bonae.logistics.user.presentation.dto.request.UserSearchCondition;
import com.bonae.logistics.user.presentation.dto.request.UserUpdateRequest;
import com.bonae.logistics.user.presentation.dto.response.*;
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
    private final CurrentAuditorProvider currentAuditorProvider;

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

    // 사용자 단건 조회. MASTER는 전체, 그 외 역할은 본인 것만 볼 수 있다.
    public UserDetailResponse getUser(UUID userId, String requesterUsername, Role requesterRole) {
        User user = findActive(userId);
        validateSelfOrMaster(user, requesterUsername, requesterRole);
        return UserDetailResponse.from(user);
    }

    // 논리 삭제된 사용자는 존재하지 않는 것으로 본다. 수정·삭제에서도 공용으로 쓴다.
    private User findActive(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    // 존재 확인 후 권한을 본다. 404/403이 갈리면 계정 존재 여부가 새어나가지만,
    // userId는 MASTER만 알 수 있는 값이라 여기서는 응답 일관성을 우선한다.
    private void validateSelfOrMaster(User user, String requesterUsername, Role requesterRole) {
        if (requesterRole == Role.MASTER) {
            return;
        }
        if (!user.getUsername().equals(requesterUsername)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    // 사용자 수정 (MASTER 전용). 더티 체킹으로 반영된다.
    @Transactional
    public UserDetailResponse updateUser(UUID userId, UserUpdateRequest request) {
        User user = findActive(userId);
        user.update(
                request.getName(),
                request.getSlackId(),
                request.getRole(),
                request.getHubId(),
                request.getCompanyId()
        );
        return UserDetailResponse.from(user);
    }

    // 사용자 논리 삭제 (MASTER 전용). 물리 삭제하지 않고 deleted_at, deleted_by만 기록한다.
    @Transactional
    public void deleteUser(UUID userId) {
        User user = findActive(userId);
        String requester = currentAuditorProvider.getCurrentAuditorOrSystem(); // = X-User-Id(username)

        // 마스터가 자기 계정을 지우면 관리 주체가 사라질 수 있어 막는다.
        if (user.getUsername().equals(requester)) {
            throw new BusinessException(ErrorCode.SELF_DELETE_NOT_ALLOWED);
        }

        user.delete(requester); // BaseEntity.delete()
    }

    // 가입 요청 목록. dev에 있는 searchUsers 쿼리를 role·hubId 조건 없이 재사용한다.
    public PageResponseDto<SignupRequestSummaryResponse> searchSignupRequests(
            SignupRequestSearchCondition condition,
            PageRequestDto pageRequestDto
    ) {
        Page<User> users = userRepository.searchUsers(
                condition.getKeyword(),
                null, // role: 가입 요청 시점엔 확정되지 않아 조건으로 쓰지 않는다
                condition.toStatus(),
                null, // hubId: 동일
                pageRequestDto.toPageable()
        );

        return PageResponseDto.from(users, SignupRequestSummaryResponse::from);
    }
}
