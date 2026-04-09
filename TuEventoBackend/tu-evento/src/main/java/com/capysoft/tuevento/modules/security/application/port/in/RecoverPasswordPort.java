package com.capysoft.tuevento.modules.security.application.port.in;

import com.capysoft.tuevento.modules.security.application.dto.request.RecoverPasswordRequest;

public interface RecoverPasswordPort {

    void recover(RecoverPasswordRequest request);
}
