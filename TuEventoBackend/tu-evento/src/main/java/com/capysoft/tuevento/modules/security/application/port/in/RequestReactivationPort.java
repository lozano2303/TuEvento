package com.capysoft.tuevento.modules.security.application.port.in;

import com.capysoft.tuevento.modules.security.application.dto.request.ReactivateAccountRequest;

public interface RequestReactivationPort {

    void request(ReactivateAccountRequest request);
}
