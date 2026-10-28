package com.capysoft.geolocation.application.port.in;

import java.util.List;

import com.capysoft.geolocation.application.dto.response.SiteResponse;

public interface GetSitesByDepartmentPort {

    List<SiteResponse> getSitesByDepartment(Integer departmentId);
}
