package com.capysoft.tuevento.modules.section.application.port.in;

import com.capysoft.tuevento.modules.section.domain.model.SectionCategory;

import com.capysoft.tuevento.modules.section.application.dto.request.CreateSectionTypeRequest;
import com.capysoft.tuevento.modules.section.application.dto.request.UpdateSectionTypeRequest;
import com.capysoft.tuevento.modules.section.application.dto.response.SectionTypeResponse;

import java.util.List;

public interface SectionTypeUseCase {
    /** Todos los tipos, o solo los de una categoría si se indica */
    List<SectionTypeResponse> getAllSectionTypes(SectionCategory category);
    SectionTypeResponse createSectionType(CreateSectionTypeRequest request);
    SectionTypeResponse updateSectionType(Integer sectionTypeId, UpdateSectionTypeRequest request);
    void activateSectionType(Integer sectionTypeId);
    void deactivateSectionType(Integer sectionTypeId);
}
