package com.capysoft.tuevento.modules.security.application.port.in;

import com.capysoft.tuevento.modules.security.application.dto.request.LoginRequest;
import com.capysoft.tuevento.modules.security.application.dto.response.LoginResponse;

public interface LoginPort {

    LoginResponse login(LoginRequest request);
}
