package com.capysoft.tuevento.modules.geolocation.application.port.in;

import java.math.BigDecimal;
import java.util.List;

import com.capysoft.tuevento.modules.geolocation.application.dto.response.SiteResponse;

public interface GetNearbySitesPort {

    List<SiteResponse> getNearbySites(BigDecimal latitude, BigDecimal longitude, double radiusKm);
}
