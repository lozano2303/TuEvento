package com.capysoft.tuevento.modules.geolocation.application.port.in;

import java.util.List;

import com.capysoft.tuevento.modules.geolocation.application.dto.response.DepartmentResponse;

public interface GetDepartmentsPort {

    List<DepartmentResponse> getDepartments();
}
