package com.capysoft.geolocation.application.port.in;

import java.util.List;

import com.capysoft.geolocation.application.dto.response.CityResponse;

public interface GetCitiesByDepartmentPort {

    List<CityResponse> getCitiesByDepartment(Integer departmentId);
}
