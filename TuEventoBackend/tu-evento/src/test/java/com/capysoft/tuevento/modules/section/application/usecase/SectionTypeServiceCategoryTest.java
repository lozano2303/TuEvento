package com.capysoft.tuevento.modules.section.application.usecase;

import com.capysoft.tuevento.modules.section.application.dto.request.CreateSectionTypeRequest;
import com.capysoft.tuevento.modules.section.application.dto.request.UpdateSectionTypeRequest;
import com.capysoft.tuevento.modules.section.application.dto.response.SectionTypeResponse;
import com.capysoft.tuevento.modules.section.domain.model.SectionCategory;
import com.capysoft.tuevento.modules.section.domain.model.SectionType;
import com.capysoft.tuevento.modules.section.domain.repository.SectionTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SectionTypeService — categorías del catálogo")
class SectionTypeServiceCategoryTest {

    @Mock SectionTypeRepository sectionTypeRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks SectionTypeService service;

    private static SectionType type(int id, String name, SectionCategory category) {
        return SectionType.builder().sectionTypeId(id).name(name).active(true).category(category).build();
    }

    private SectionType savedType() {
        ArgumentCaptor<SectionType> captor = ArgumentCaptor.forClass(SectionType.class);
        verify(sectionTypeRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("lista todo o filtra por categoría, e informa la categoría")
    void listsAndFiltersByCategory() {
        when(sectionTypeRepository.findAll()).thenReturn(List.of(
                type(1, "General", SectionCategory.GENERAL),
                type(2, "Palco Diamante", SectionCategory.PALCO),
                type(3, "Palco Oro", SectionCategory.PALCO)));

        assertThat(service.getAllSectionTypes(null)).hasSize(3);
        List<SectionTypeResponse> palcos = service.getAllSectionTypes(SectionCategory.PALCO);
        assertThat(palcos).extracting(SectionTypeResponse::getName).containsExactly("Palco Diamante", "Palco Oro");
        assertThat(palcos).allSatisfy(r -> assertThat(r.getCategory()).isEqualTo(SectionCategory.PALCO));
    }

    @Test
    @DisplayName("crear sin categoría usa GENERAL")
    void createDefaultsToGeneral() {
        when(sectionTypeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createSectionType(CreateSectionTypeRequest.builder().name("Nueva").build());

        assertThat(savedType().getCategory()).isEqualTo(SectionCategory.GENERAL);
    }

    @Test
    @DisplayName("crear con categoría la respeta")
    void createKeepsGivenCategory() {
        when(sectionTypeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createSectionType(CreateSectionTypeRequest.builder().name("Palco Bronce").category(SectionCategory.PALCO).build());

        assertThat(savedType().getCategory()).isEqualTo(SectionCategory.PALCO);
    }

    @Test
    @DisplayName("actualizar el nombre sin categoría conserva la actual; con categoría la cambia")
    void updateKeepsOrChangesCategory() {
        when(sectionTypeRepository.findById(2)).thenReturn(Optional.of(type(2, "Palco Diamante", SectionCategory.PALCO)));
        when(sectionTypeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateSectionTypeRequest keep = new UpdateSectionTypeRequest();
        ReflectionTestUtils.setField(keep, "name", "Palco Diamante VIP");
        SectionTypeResponse kept = service.updateSectionType(2, keep);
        assertThat(kept.getCategory()).isEqualTo(SectionCategory.PALCO);

        UpdateSectionTypeRequest change = new UpdateSectionTypeRequest();
        ReflectionTestUtils.setField(change, "name", "Palco Diamante");
        ReflectionTestUtils.setField(change, "category", SectionCategory.VIP);
        assertThat(service.updateSectionType(2, change).getCategory()).isEqualTo(SectionCategory.VIP);
    }

    @Test
    @DisplayName("activar y desactivar no pierden la categoría")
    void activationKeepsCategory() {
        when(sectionTypeRepository.findById(2)).thenReturn(Optional.of(type(2, "Palco Diamante", SectionCategory.PALCO)));

        service.deactivateSectionType(2);
        SectionType deactivated = savedType();
        assertThat(deactivated.getActive()).isFalse();
        assertThat(deactivated.getCategory()).isEqualTo(SectionCategory.PALCO);
    }

    @Test
    @DisplayName("activar conserva la categoría")
    void activateKeepsCategory() {
        when(sectionTypeRepository.findById(2)).thenReturn(Optional.of(type(2, "Palco Diamante", SectionCategory.PALCO)));

        service.activateSectionType(2);
        SectionType activated = savedType();
        assertThat(activated.getActive()).isTrue();
        assertThat(activated.getCategory()).isEqualTo(SectionCategory.PALCO);
    }
}
