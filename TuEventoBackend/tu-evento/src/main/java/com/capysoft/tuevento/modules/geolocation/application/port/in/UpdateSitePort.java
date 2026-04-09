package com.capysoft.tuevento.modules.geolocation.application.port.in;

import com.capysoft.tuevento.modules.geolocation.application.dto.request.UpdateSiteRequest;
import com.capysoft.tuevento.modules.geolocation.application.dto.response.SiteResponse;

public interface UpdateSitePort {

    SiteResponse updateSite(Integer siteId, UpdateSiteRequest request);
}
