package com.capysoft.tuevento.modules.security.application.port.in;

import com.capysoft.tuevento.modules.security.application.dto.request.GoogleAuthRequest;
import com.capysoft.tuevento.modules.security.application.dto.response.LoginResponse;

public interface GoogleAuthPort {

    LoginResponse authenticate(GoogleAuthRequest request);
}
