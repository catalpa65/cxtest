package com.example.ecommerce.common.error;

public enum ErrorCode {
    BAD_REQUEST("BAD_REQUEST"),
    UNAUTHORIZED("UNAUTHORIZED"),
    NOT_FOUND("NOT_FOUND"),
    CONFLICT("CONFLICT"),
    INTERNAL_ERROR("INTERNAL_ERROR");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
