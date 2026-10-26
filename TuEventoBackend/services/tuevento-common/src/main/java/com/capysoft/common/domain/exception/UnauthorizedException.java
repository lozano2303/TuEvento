package com.capysoft.common.domain.exception;

/** Business rule that requires an authenticated user; reported as HTTP 401 while keeping its code. */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String code, String message) {
        super(code, message);
    }
}
