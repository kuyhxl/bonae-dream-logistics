package com.bonae.logistics.message.infrastructure.slack.dto;

/* channel에 유저의 슬랙ID를 넣으면 해당 유저와의 DM이 자동으로 열린다. */
public record SlackPostMessageRequest(String channel, String text) {
}
