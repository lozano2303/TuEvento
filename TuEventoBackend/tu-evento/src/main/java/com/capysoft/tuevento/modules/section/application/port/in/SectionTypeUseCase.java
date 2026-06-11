package com.capysoft.tuevento.modules.section.application.port.in;

import com.capysoft.tuevento.modules.section.application.dto.request.CreateSectionTypeRequest;
import com.capysoft.tuevento.modules.section.application.dto.response.SectionTypeResponse;

import java.util.List;

public interface SectionTypeUseCase {
    List<SectionTypeResponse> getAllSectionTypes();
    SectionTypeResponse createSectionType(CreateSectionTypeRequest request);
}
