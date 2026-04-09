package com.capysoft.tuevento.modules.geolocation.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentResponse {

    private final Integer departmentId;
    private final String name;
    private final String code;
}
