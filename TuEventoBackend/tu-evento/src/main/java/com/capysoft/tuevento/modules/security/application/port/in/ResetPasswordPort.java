package com.capysoft.tuevento.modules.security.application.port.in;

import com.capysoft.tuevento.modules.security.application.dto.request.ResetPasswordRequest;

public interface ResetPasswordPort {

    void reset(ResetPasswordRequest request);
}
