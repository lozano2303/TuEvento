package com.capysoft.identity.security.application.port.in;

import com.capysoft.identity.security.application.dto.request.ConfirmReactivationRequest;

public interface ConfirmReactivationPort {

    void confirm(ConfirmReactivationRequest request);
}
