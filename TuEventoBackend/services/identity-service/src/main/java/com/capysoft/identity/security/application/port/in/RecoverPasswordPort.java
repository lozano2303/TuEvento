package com.capysoft.identity.security.application.port.in;

import com.capysoft.identity.security.application.dto.request.RecoverPasswordRequest;

public interface RecoverPasswordPort {

    void recover(RecoverPasswordRequest request);
}
