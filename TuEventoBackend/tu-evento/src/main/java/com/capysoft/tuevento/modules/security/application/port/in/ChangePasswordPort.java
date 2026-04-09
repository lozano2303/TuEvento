package com.capysoft.tuevento.modules.security.application.port.in;

import com.capysoft.tuevento.modules.security.application.dto.request.ChangePasswordRequest;

public interface ChangePasswordPort {

    void change(ChangePasswordRequest request);
}
