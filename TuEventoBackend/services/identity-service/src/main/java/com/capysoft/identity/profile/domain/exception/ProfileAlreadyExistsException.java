package com.capysoft.identity.profile.domain.exception;

import com.capysoft.common.domain.exception.BusinessException;

public class ProfileAlreadyExistsException extends BusinessException {

    public ProfileAlreadyExistsException(Integer userId) {
        super("PROFILE_ALREADY_EXISTS", "A profile already exists for user id: " + userId);
    }
}
