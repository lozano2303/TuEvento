package com.capysoft.tuevento.modules.security.application.port.in;

import com.capysoft.tuevento.modules.security.application.dto.request.DeactivateAccountRequest;

public interface DeactivateAccountPort {

    void deactivate(DeactivateAccountRequest request);
}
