package com.bonae.logistics.message.infrastructure.slack;

/*
 * 슬랙 mrkdwn 제어 문자를 무력화한다.
 *
 * 슬랙은 <!channel>, <!here> 로 전체 멘션을 걸 수 있고
 * <https://주소|보이는 텍스트> 로 링크 텍스트를 위장할 수 있다.
 * 봇이 보낸 메시지는 사내 공지로 신뢰되기 때문에, 그대로 통과시키면 피싱 경로가 된다.
 *
 * 슬랙이 규정한 이스케이프 대상은 & < > 세 개뿐이다.
 * &를 가장 먼저 바꿔야 뒤에서 만들어낸 &lt; &gt; 가 다시 치환되지 않는다.
 */
public final class SlackTextEscaper {

    private SlackTextEscaper() {
    }

    public static String escape(String text) {
        if (text == null) {
            return null;
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}