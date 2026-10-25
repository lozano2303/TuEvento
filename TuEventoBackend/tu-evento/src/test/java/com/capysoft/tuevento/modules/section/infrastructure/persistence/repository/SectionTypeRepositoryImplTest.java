package com.capysoft.tuevento.modules.section.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.section.domain.model.SectionCategory;
import com.capysoft.tuevento.modules.section.domain.model.SectionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistencia de la categoría de los tipos de sección contra una base real
 * (H2 en modo PostgreSQL del perfil test).
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SectionTypeRepositoryImpl.class)
@DisplayName("SectionTypeRepositoryImpl — categoría")
class SectionTypeRepositoryImplTest {

    @Autowired SectionTypeRepositoryImpl repository;

    @Test
    @DisplayName("guarda y lee la categoría del tipo de sección")
    void savesAndReadsCategory() {
        SectionType saved = repository.save(SectionType.builder()
                .name("Palco Diamante").active(true).category(SectionCategory.PALCO).build());

        assertThat(saved.getSectionTypeId()).isNotNull();
        assertThat(saved.getCategory()).isEqualTo(SectionCategory.PALCO);
        assertThat(repository.findById(saved.getSectionTypeId()))
                .hasValueSatisfying(type -> assertThat(type.getCategory()).isEqualTo(SectionCategory.PALCO));
        assertThat(repository.findAll()).extracting(SectionType::getCategory).containsExactly(SectionCategory.PALCO);
    }

    @Test
    @DisplayName("sin categoría se guarda como GENERAL")
    void defaultsToGeneral() {
        SectionType saved = repository.save(SectionType.builder().name("Gramilla").active(true).build());

        assertThat(saved.getCategory()).isEqualTo(SectionCategory.GENERAL);
        assertThat(repository.existsByName("Gramilla")).isTrue();
    }
}
