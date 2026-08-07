package com.bonae.logistics.user.presentation.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SignupRequest 입력 검증")
class SignupRequestTest {

    private static final String VALID_USERNAME = "testuser";
    private static final String VALID_PASSWORD = "Test1234!";
    private static final String VALID_NAME = "테스트";
    private static final String VALID_SLACK_ID = "U000TEST123";
    private static final String VALID_AFFILIATION = "테스트업체";

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private SignupRequest request(String username, String password) {
        return new SignupRequest(username, password, VALID_NAME, VALID_SLACK_ID, VALID_AFFILIATION);
    }

    private Set<ConstraintViolation<SignupRequest>> validate(SignupRequest request) {
        return validator.validate(request);
    }

    private boolean hasViolationOn(Set<ConstraintViolation<SignupRequest>> violations, String field) {
        return violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals(field));
    }

    @Test
    @DisplayName("모든 값이 규칙에 맞으면 위반이 없다")
    void valid() {
        assertThat(validate(request(VALID_USERNAME, VALID_PASSWORD))).isEmpty();
    }

    // ---------- password ----------

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
            "Test1234!",
            "Abcd123!@",
            "Password1!",
            "aB3!aB3!"      // 정확히 8자 (하한 경계)
    })
    @DisplayName("유효한 비밀번호는 통과한다")
    void validPassword(String password) {
        assertThat(validate(request(VALID_USERNAME, password))).isEmpty();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
            "Abcdefg!",         // 숫자 없음
            "abcd1234!",        // 대문자 없음
            "ABCD1234!",        // 소문자 없음
            "Abcd1234",         // 특수문자 없음
            "Ab3!Ab3",          // 7자 (하한 미달)
            "Abcd1234!Abcd123",  // 16자 (상한 초과)
            "Abcd1234! ",       // 공백 포함
            "Abcd1234!가나다"     // 한글 포함
    })
    @DisplayName("규칙을 어긴 비밀번호는 위반이 발생한다")
    void invalidPassword(String password) {
        assertThat(hasViolationOn(validate(request(VALID_USERNAME, password)), "password")).isTrue();
    }

    // ---------- username ----------

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
            "abcd",         // 4자 (하한 경계)
            "testuser",
            "abcdefghij"    // 10자 (상한 경계)
    })
    @DisplayName("유효한 아이디는 통과한다")
    void validUsername(String username) {
        assertThat(validate(request(username, VALID_PASSWORD))).isEmpty();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
            "abc",           // 3자 (하한 미달)
            "abcdefghijk",   // 11자 (상한 초과)
            "TestUser",      // 대문자 포함
            "test_user",     // 특수문자 포함
            "테스트유저"          // 한글 포함
    })
    @DisplayName("규칙을 어긴 아이디는 위반이 발생한다")
    void invalidUsername(String username) {
        assertThat(hasViolationOn(validate(request(username, VALID_PASSWORD)), "username")).isTrue();
    }

    // ---------- 필수값 ----------

    @Test
    @DisplayName("이름이 공백이면 위반이 발생한다")
    void blankName() {
        SignupRequest request = new SignupRequest(
                VALID_USERNAME, VALID_PASSWORD, "   ", VALID_SLACK_ID, VALID_AFFILIATION);

        assertThat(hasViolationOn(validate(request), "name")).isTrue();
    }

    @Test
    @DisplayName("슬랙 ID가 null이면 위반이 발생한다")
    void nullSlackId() {
        SignupRequest request = new SignupRequest(
                VALID_USERNAME, VALID_PASSWORD, VALID_NAME, null, VALID_AFFILIATION);

        assertThat(hasViolationOn(validate(request), "slackId")).isTrue();
    }

    @Test
    @DisplayName("소속명이 null이면 위반이 발생한다")
    void nullAffiliationName() {
        SignupRequest request = new SignupRequest(
                VALID_USERNAME, VALID_PASSWORD, VALID_NAME, VALID_SLACK_ID, null);

        assertThat(hasViolationOn(validate(request), "affiliationName")).isTrue();
    }
}