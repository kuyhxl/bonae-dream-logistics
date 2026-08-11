package com.bonae.logistics.message.infrastructure.ai;

/*
 * AI 발송시한 산출 창구.
 * 호출부는 이 인터페이스 외에 Gemini를 직접 알지 않는다(모델 교체 지점).
 */
public interface AiDeadlineClient {
    AiDeadlineResult generate(String prompt);
}