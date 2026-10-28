package com.capysoft.identity.security.application.port.in;

import com.capysoft.identity.security.application.dto.request.ReactivateAccountRequest;

public interface RequestReactivationPort {

    void request(ReactivateAccountRequest request);
}
