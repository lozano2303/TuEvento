package com.capysoft.identity.security.application.port.in;

import com.capysoft.identity.security.application.dto.request.ChangePasswordRequest;

public interface ChangePasswordPort {

    void change(ChangePasswordRequest request);
}
