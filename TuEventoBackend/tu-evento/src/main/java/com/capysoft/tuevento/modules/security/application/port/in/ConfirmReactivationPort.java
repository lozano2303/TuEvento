package com.capysoft.tuevento.modules.security.application.port.in;

import com.capysoft.tuevento.modules.security.application.dto.request.ConfirmReactivationRequest;

public interface ConfirmReactivationPort {

    void confirm(ConfirmReactivationRequest request);
}
