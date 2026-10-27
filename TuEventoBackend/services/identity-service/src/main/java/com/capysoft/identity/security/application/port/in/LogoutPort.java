package com.capysoft.identity.security.application.port.in;

import com.capysoft.identity.security.application.dto.request.LogoutRequest;

public interface LogoutPort {

    void logout(LogoutRequest request);
}
