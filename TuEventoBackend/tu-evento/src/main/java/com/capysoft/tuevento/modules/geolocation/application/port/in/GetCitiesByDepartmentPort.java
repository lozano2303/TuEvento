package com.capysoft.tuevento.modules.geolocation.application.port.in;

import java.util.List;

import com.capysoft.tuevento.modules.geolocation.application.dto.response.CityResponse;

public interface GetCitiesByDepartmentPort {

    List<CityResponse> getCitiesByDepartment(Integer departmentId);
}
