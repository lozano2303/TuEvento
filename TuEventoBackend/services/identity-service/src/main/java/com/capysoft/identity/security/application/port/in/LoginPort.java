package com.capysoft.identity.security.application.port.in;

import com.capysoft.identity.security.application.dto.request.LoginRequest;
import com.capysoft.identity.security.application.dto.response.LoginResponse;

public interface LoginPort {

    LoginResponse login(LoginRequest request);
}
