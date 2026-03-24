package com.capysoft.tuevento.shared.domain.exception;

public class NotFoundException extends BusinessException {

    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
