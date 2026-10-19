package com.capysoft.tuevento.modules.section.application.usecase;

import com.capysoft.tuevento.modules.section.application.dto.request.CreateSectionTypeRequest;
import com.capysoft.tuevento.modules.section.application.dto.request.UpdateSectionTypeRequest;
import com.capysoft.tuevento.modules.section.application.dto.response.SectionTypeResponse;
import com.capysoft.tuevento.modules.section.application.port.in.SectionTypeUseCase;
import com.capysoft.tuevento.modules.section.domain.model.SectionType;
import com.capysoft.tuevento.modules.section.domain.repository.SectionTypeRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
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
                .active(true)
                .build());
        return toResponse(saved);
    }

    @Override
    public SectionTypeResponse updateSectionType(Integer sectionTypeId, UpdateSectionTypeRequest request) {
        SectionType sectionType = sectionTypeRepository.findById(sectionTypeId)
                .orElseThrow(() -> new NotFoundException("SECTION_TYPE_NOT_FOUND", "Tipo de sección no encontrado"));

        SectionType updated = sectionTypeRepository.save(SectionType.builder()
                .sectionTypeId(sectionType.getSectionTypeId())
                .name(request.getName())
                .active(sectionType.getActive())
                .build());

        return toResponse(updated);
    }

    @Override
    public void activateSectionType(Integer sectionTypeId) {
        SectionType sectionType = sectionTypeRepository.findById(sectionTypeId)
                .orElseThrow(() -> new NotFoundException("SECTION_TYPE_NOT_FOUND", "Tipo de sección no encontrado"));

        sectionTypeRepository.save(SectionType.builder()
                .sectionTypeId(sectionType.getSectionTypeId())
                .name(sectionType.getName())
                .active(true)
                .build());
    }

    @Override
    public void deactivateSectionType(Integer sectionTypeId) {
        SectionType sectionType = sectionTypeRepository.findById(sectionTypeId)
                .orElseThrow(() -> new NotFoundException("SECTION_TYPE_NOT_FOUND", "Tipo de sección no encontrado"));

        sectionTypeRepository.save(SectionType.builder()
                .sectionTypeId(sectionType.getSectionTypeId())
                .name(sectionType.getName())
                .active(false)
                .build());
    }

    private SectionTypeResponse toResponse(SectionType domain) {
        return SectionTypeResponse.builder()
                .sectionTypeId(domain.getSectionTypeId())
                .name(domain.getName())
                .active(domain.getActive())
                .build();
    }
}
