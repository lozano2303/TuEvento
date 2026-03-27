package com.capysoft.tuevento.modules.security.application.port.in;

import com.capysoft.tuevento.modules.security.application.dto.request.RegisterUserRequest;
import com.capysoft.tuevento.modules.security.application.dto.response.RegisterUserResponse;

public interface RegisterUserPort {

    RegisterUserResponse register(RegisterUserRequest request);
}
