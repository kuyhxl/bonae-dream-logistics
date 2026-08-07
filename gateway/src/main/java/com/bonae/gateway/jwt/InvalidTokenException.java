package com.bonae.gateway.jwt;

public class InvalidTokenException extends RuntimeException{

    public enum Reason {
        MALFORMED,
        SIGNATURE_INVALID,
        EXPIRED,
        NOT_ACCESS_TOKEN,
        CLAIM_MISSING,
        ROLE_NOT_ALLOWED
    }

    private final Reason reason;

    public InvalidTokenException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
