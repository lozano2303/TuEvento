package com.capysoft.identity.security.application.port.in;

import com.capysoft.identity.security.application.dto.request.RegisterUserRequest;
import com.capysoft.identity.security.application.dto.response.RegisterUserResponse;

public interface RegisterUserPort {

    RegisterUserResponse register(RegisterUserRequest request);
    void resendActivationCode(String email);
}
