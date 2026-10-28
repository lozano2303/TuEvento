package com.capysoft.identity.security.application.port.in;

import com.capysoft.identity.security.application.dto.request.ResetPasswordRequest;

public interface ResetPasswordPort {

    void reset(ResetPasswordRequest request);
}
