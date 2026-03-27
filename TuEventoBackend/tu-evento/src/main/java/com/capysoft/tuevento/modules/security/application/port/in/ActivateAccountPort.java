package com.capysoft.tuevento.modules.security.application.port.in;

import com.capysoft.tuevento.modules.security.application.dto.request.ActivateAccountRequest;

public interface ActivateAccountPort {

    void activate(ActivateAccountRequest request);
}
