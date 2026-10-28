package com.capysoft.identity.security.application.port.in;

import com.capysoft.identity.security.application.dto.request.DeactivateAccountRequest;

public interface DeactivateAccountPort {

    void deactivate(DeactivateAccountRequest request);
}
