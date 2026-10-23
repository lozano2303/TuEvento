package com.capysoft.tuevento.modules.security.application.port.in;

import com.capysoft.tuevento.modules.security.application.dto.request.LogoutRequest;

public interface LogoutPort {

    void logout(LogoutRequest request);
}
