package com.capysoft.tuevento.modules.geolocation.application.port.in;

import com.capysoft.tuevento.modules.geolocation.application.dto.request.CreateSiteRequest;
import com.capysoft.tuevento.modules.geolocation.application.dto.response.SiteResponse;

public interface CreateSitePort {

    SiteResponse createSite(CreateSiteRequest request);
}
