package com.capysoft.identity.security.application.port.in;

import com.capysoft.identity.security.application.dto.request.ActivateAccountRequest;

public interface ActivateAccountPort {

    void activate(ActivateAccountRequest request);
}
