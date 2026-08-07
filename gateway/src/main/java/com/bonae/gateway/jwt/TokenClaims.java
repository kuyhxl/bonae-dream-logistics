package com.bonae.gateway.jwt;

public record TokenClaims(
        String username,
        String role,
        String jti) {

}
