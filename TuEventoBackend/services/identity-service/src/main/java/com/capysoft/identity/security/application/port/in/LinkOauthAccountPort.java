package com.capysoft.identity.security.application.port.in;

import com.capysoft.identity.security.application.dto.request.LinkOauthAccountRequest;
import com.capysoft.identity.security.application.dto.response.LinkOauthAccountResponse;

public interface LinkOauthAccountPort {

    LinkOauthAccountResponse link(LinkOauthAccountRequest request);
}
