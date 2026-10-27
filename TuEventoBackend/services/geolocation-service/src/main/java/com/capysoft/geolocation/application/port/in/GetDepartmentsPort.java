package com.capysoft.geolocation.application.port.in;

import java.util.List;

import com.capysoft.geolocation.application.dto.response.DepartmentResponse;

public interface GetDepartmentsPort {

    List<DepartmentResponse> getDepartments();
}
