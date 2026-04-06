package com.capysoft.tuevento.modules.geolocation.application.port.in;

import com.capysoft.tuevento.modules.geolocation.application.dto.response.SiteResponse;

public interface GetSitePort {

    SiteResponse getSite(Integer siteId);
}
