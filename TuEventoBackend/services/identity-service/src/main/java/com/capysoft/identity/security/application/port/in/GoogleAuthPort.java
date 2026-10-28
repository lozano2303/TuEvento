package com.capysoft.identity.security.application.port.in;

import com.capysoft.identity.security.application.dto.request.GoogleAuthRequest;
import com.capysoft.identity.security.application.dto.response.LoginResponse;

public interface GoogleAuthPort {

    LoginResponse authenticate(GoogleAuthRequest request);
}
