package com.capysoft.common.domain.exception;

/** Business rule violation that must be reported as HTTP 403 while keeping its code. */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(String code, String message) {
        super(code, message);
    }
}
