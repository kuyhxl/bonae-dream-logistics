package com.bonae.logistics.message.infrastructure.ai.dto;

import java.util.List;

public record GeminiGenerateRequest(List<Content> contents, GenerationConfig generationConfig) {

    public record Content(List<Part> parts) {}

    public record Part(String text) {}

    public record GenerationConfig(double temperature, String responseMimeType) {}

    /* 같은 입력에 같은 시한이 나와야 하므로 temperature는 0, 응답은 JSON으로 강제한다. */
    public static GeminiGenerateRequest of(String prompt) {
        return new GeminiGenerateRequest(
                List.of(new Content(List.of(new Part(prompt)))),
                new GenerationConfig(0.0, "application/json")
        );
    }
}