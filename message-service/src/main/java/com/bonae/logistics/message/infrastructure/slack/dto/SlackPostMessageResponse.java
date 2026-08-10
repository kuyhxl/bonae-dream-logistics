package com.bonae.logistics.message.infrastructure.slack.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SlackPostMessageResponse(boolean ok, String error) {
}
