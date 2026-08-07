package com.bonae.logistics.user.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.user.domain.entity.Status;
import com.bonae.logistics.user.domain.entity.User;
import com.bonae.logistics.user.domain.repository.UserRepository;
import com.bonae.logistics.user.presentation.dto.request.SignupRequest;
import com.bonae.logistics.user.presentation.dto.response.SignupResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 회원가입")
class AuthServiceTest {

    private static final String USERNAME = "testuser";
    private static final String RAW_PASSWORD = "Test1234!";
    private static final String ENCODED_PASSWORD = "$2a$10$encoded";

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private SignupRequest request() {
        return new SignupRequest(USERNAME, RAW_PASSWORD, "테스트", "U000TEST123", "테스트업체");
    }

    @Test
    @DisplayName("회원가입에 성공하면 PENDING 상태로 저장되고 권한은 비어 있다")
    void signup_success() {
        // given
        given(userRepository.existsByUsername(USERNAME)).willReturn(false);
        given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(ENCODED_PASSWORD);
        given(userRepository.save(any(User.class))).willAnswer(i -> i.getArgument(0));

        // when
        SignupResponse response = authService.signup(request());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        then(userRepository).should().save(captor.capture());
        User saved = captor.getValue();

        // then
        assertThat(saved.getUsername()).isEqualTo(USERNAME);
        assertThat(saved.getStatus()).isEqualTo(Status.PENDING);
        assertThat(saved.getRole()).isNull();
        assertThat(response.getUsername()).isEqualTo(USERNAME);
        assertThat(response.getStatus()).isEqualTo(Status.PENDING);
    }

    @Test
    @DisplayName("비밀번호는 암호화된 값으로 저장된다")
    void signup_encodesPassword() {
        // given
        given(userRepository.existsByUsername(USERNAME)).willReturn(false);
        given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(ENCODED_PASSWORD);
        given(userRepository.save(any(User.class))).willAnswer(i -> i.getArgument(0));

        // when
        authService.signup(request());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        then(userRepository).should().save(captor.capture());
        User saved = captor.getValue();

        // then
        assertThat(saved.getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(saved.getPassword()).isNotEqualTo(RAW_PASSWORD);
    }

    @Test
    @DisplayName("문자열 필드의 앞뒤 공백은 제거되어 저장된다")
    void signup_trimsStringFields() {
        // given
        SignupRequest request =
                new SignupRequest(USERNAME, RAW_PASSWORD, "  테스트  ", "  U000TEST123  ", "  테스트업체  ");
        given(userRepository.existsByUsername(USERNAME)).willReturn(false);
        given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(ENCODED_PASSWORD);
        given(userRepository.save(any(User.class))).willAnswer(i -> i.getArgument(0));

        // when
        authService.signup(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        then(userRepository).should().save(captor.capture());
        User saved = captor.getValue();

        // then
        assertThat(saved.getName()).isEqualTo("테스트");
        assertThat(saved.getSlackId()).isEqualTo("U000TEST123");
        assertThat(saved.getAffiliationName()).isEqualTo("테스트업체");
    }

    @Test
    @DisplayName("이미 사용 중인 아이디면 저장을 시도하지 않고 예외가 발생한다")
    void signup_duplicatedUsername() {
        // given
        given(userRepository.existsByUsername(USERNAME)).willReturn(true);

        // when - then
        assertThatThrownBy(() -> authService.signup(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USERNAME_DUPLICATED);

        then(userRepository).should(never()).save(any());
        then(passwordEncoder).should(never()).encode(anyString());
    }

    @Test
    @DisplayName("동시 요청으로 저장 중 유니크 제약을 위반하면 중복 예외로 변환된다")
    void signup_concurrentDuplicate() {
        // given
        given(userRepository.existsByUsername(USERNAME)).willReturn(false);
        given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(ENCODED_PASSWORD);
        given(userRepository.save(any(User.class)))
                .willThrow(new DataIntegrityViolationException("유니크 제약 위반"));

        // when - then
        assertThatThrownBy(() -> authService.signup(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USERNAME_DUPLICATED);
    }
}