package com.capysoft.tuevento.modules.section.application.usecase;

import com.capysoft.tuevento.modules.section.application.dto.request.CreateSectionTypeRequest;
import com.capysoft.tuevento.modules.section.application.dto.response.SectionTypeResponse;
import com.capysoft.tuevento.modules.section.application.port.in.SectionTypeUseCase;
import com.capysoft.tuevento.modules.section.domain.model.SectionType;
import com.capysoft.tuevento.modules.section.domain.repository.SectionTypeRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SectionTypeService implements SectionTypeUseCase {

    private final SectionTypeRepository sectionTypeRepository;

    @Override
    public List<SectionTypeResponse> getAllSectionTypes() {
        return sectionTypeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public SectionTypeResponse createSectionType(CreateSectionTypeRequest request) {
        if (sectionTypeRepository.existsByName(request.getName())) {
            throw new BusinessException("SECTION_TYPE_ALREADY_EXISTS",
                    "Ya existe un tipo de sección con ese nombre");
        }
        SectionType saved = sectionTypeRepository.save(SectionType.builder()
                .name(request.getName())
                .build());
        return toResponse(saved);
    }

    private SectionTypeResponse toResponse(SectionType domain) {
        return SectionTypeResponse.builder()
                .sectionTypeId(domain.getSectionTypeId())
                .name(domain.getName())
                .build();
    }
}
