package com.capysoft.tuevento.modules.security.application.port.in;

import com.capysoft.tuevento.modules.security.application.dto.request.LinkOauthAccountRequest;
import com.capysoft.tuevento.modules.security.application.dto.response.LinkOauthAccountResponse;

public interface LinkOauthAccountPort {

    LinkOauthAccountResponse link(LinkOauthAccountRequest request);
}
