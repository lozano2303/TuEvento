package com.capysoft.tuevento.modules.geolocation.application.port.in;

import java.util.List;

import com.capysoft.tuevento.modules.geolocation.application.dto.response.SiteResponse;

public interface GetSitesByDepartmentPort {

    List<SiteResponse> getSitesByDepartment(Integer departmentId);
}
