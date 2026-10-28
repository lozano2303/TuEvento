package com.capysoft.tuevento.modules.section.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.section.infrastructure.persistence.entity.TranslationStatusEnum;
import com.capysoft.tuevento.modules.section.domain.model.SectionTypeTranslation;
import com.capysoft.tuevento.modules.section.infrastructure.persistence.entity.SectionTypeTranslationEntity;
import com.capysoft.tuevento.modules.section.infrastructure.persistence.mapper.SectionTypeTranslationMapper;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Adaptador y mapper de las traducciones de tipos de sección: la tabla guarda el estado en
 * minúsculas ({@code draft}, {@code published}, {@code pending_review}) y el origen como texto.
 */
@DisplayName("SectionTypeTranslationRepositoryImpl")
class SectionTypeTranslationRepositoryImplTest {

    private final SectionTypeTranslationJpaRepository jpa = mock(SectionTypeTranslationJpaRepository.class);
    private final SectionTypeTranslationMapper mapper = new SectionTypeTranslationMapper();
    private final SectionTypeTranslationRepositoryImpl repository = new SectionTypeTranslationRepositoryImpl(jpa, mapper);

    private static SectionTypeTranslation domain(TranslationSource source, TranslationStatus status) {
        SectionTypeTranslation t = new SectionTypeTranslation();
        t.setTranslationId(4);
        t.setSectionTypeId(1);
        t.setLanguageId(2);
        t.setTranslatedName("Box");
        t.setSource(source);
        t.setStatus(status);
        return t;
    }

    @Test
    @DisplayName("busca por tipo e idioma, guarda y borra a través del mapper")
    void findSaveDelete() {
        SectionTypeTranslationEntity stored = SectionTypeTranslationEntity.builder().translationId(4)
                .sectionTypeId(1).languageId(2).translatedName("Box").source("MACHINE")
                .status(TranslationStatusEnum.published).build();
        when(jpa.findBySectionTypeIdAndLanguageId(1, 2)).thenReturn(Optional.of(stored));
        when(jpa.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SectionTypeTranslation found = repository.findBySectionTypeAndLanguage(1L, 2).orElseThrow();
        assertThat(found.getSource()).isEqualTo(TranslationSource.MACHINE);
        assertThat(found.getStatus()).isEqualTo(TranslationStatus.PUBLISHED);
        assertThat(found).isEqualTo(domain(TranslationSource.MANUAL, TranslationStatus.DRAFT));

        SectionTypeTranslation saved = repository.save(domain(TranslationSource.MANUAL, TranslationStatus.PENDING_REVIEW));
        assertThat(saved.getStatus()).isEqualTo(TranslationStatus.PENDING_REVIEW);

        repository.delete(found);
        verify(jpa).delete(any(SectionTypeTranslationEntity.class));
    }

    @ParameterizedTest(name = "{0} ↔ {1}")
    @CsvSource({"DRAFT, draft", "PUBLISHED, published", "PENDING_REVIEW, pending_review"})
    @DisplayName("convierte el estado entre dominio y tabla en los dos sentidos")
    void statusRoundTrip(TranslationStatus status, TranslationStatusEnum stored) {
        SectionTypeTranslationEntity entity = mapper.toEntity(domain(TranslationSource.MACHINE, status));

        assertThat(entity.getStatus()).isEqualTo(stored);
        assertThat(entity.getSource()).isEqualTo("MACHINE");
        assertThat(mapper.toDomain(entity).getStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("nulos y orígenes desconocidos: manual y borrador")
    void defaults() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();

        SectionTypeTranslationEntity entity = mapper.toEntity(domain(null, null));
        assertThat(entity.getSource()).isEqualTo("MANUAL");
        assertThat(entity.getStatus()).isEqualTo(TranslationStatusEnum.draft);

        SectionTypeTranslationEntity odd = SectionTypeTranslationEntity.builder().source("robot").status(null).build();
        assertThat(mapper.toDomain(odd).getSource()).isEqualTo(TranslationSource.MANUAL);
        assertThat(mapper.toDomain(odd).getStatus()).isEqualTo(TranslationStatus.DRAFT);
        odd.setSource(null);
        assertThat(mapper.toDomain(odd).getSource()).isEqualTo(TranslationSource.MANUAL);
    }
}
