package com.bonae.logistics.message.infrastructure.slack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlackTextEscaperTest {

    @Test
    @DisplayName("전체 멘션 문법을 무력화한다")
    void escapesBroadcastMention() {
        assertThat(SlackTextEscaper.escape("<!channel> 지금 확인하세요"))
                .isEqualTo("&lt;!channel&gt; 지금 확인하세요");
    }

    @Test
    @DisplayName("링크 위장 문법을 무력화한다")
    void escapesDisguisedLink() {
        assertThat(SlackTextEscaper.escape("<https://evil.com|계정 인증하기>"))
                .isEqualTo("&lt;https://evil.com|계정 인증하기&gt;");
    }

    @Test
    @DisplayName("앰퍼샌드를 먼저 치환해 이중 이스케이프가 생기지 않는다")
    void escapesAmpersandFirst() {
        assertThat(SlackTextEscaper.escape("A & <B>"))
                .isEqualTo("A &amp; &lt;B&gt;");
    }

    @Test
    @DisplayName("일반 문장은 그대로 둔다")
    void keepsPlainText() {
        String plain = "주문 번호 : 1\n최종 발송 시한은 12월 10일 오전 9시 입니다.";
        assertThat(SlackTextEscaper.escape(plain)).isEqualTo(plain);
    }

    @Test
    @DisplayName("null은 그대로 반환한다")
    void keepsNull() {
        assertThat(SlackTextEscaper.escape(null)).isNull();
    }
}