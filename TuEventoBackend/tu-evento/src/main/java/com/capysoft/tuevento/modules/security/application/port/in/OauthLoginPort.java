package com.capysoft.tuevento.modules.security.application.port.in;

import com.capysoft.tuevento.modules.security.application.dto.response.LoginResponse;

public interface OauthLoginPort {

    LoginResponse login(String provider, String code);
}
