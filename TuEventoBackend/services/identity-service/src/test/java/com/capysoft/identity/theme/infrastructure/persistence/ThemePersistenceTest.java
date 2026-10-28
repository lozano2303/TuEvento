package com.capysoft.identity.theme.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.capysoft.identity.shared.domain.valueobject.TranslationSource;
import com.capysoft.identity.shared.domain.valueobject.TranslationStatus;
import com.capysoft.identity.shared.infrastructure.config.JpaAuditingConfig;
import com.capysoft.identity.theme.infrastructure.persistence.entity.ThemeTranslationEntity;
import com.capysoft.identity.theme.infrastructure.persistence.mapper.ThemeTranslationMapper;
import com.capysoft.identity.theme.infrastructure.persistence.repository.ThemeTranslationRepositoryImpl;

/** Las entidades de temas se mapean y la traducción se lee por tema e idioma. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:identity;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;NON_KEYWORDS=ROW,VALUE,USER,KEY",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"})
@Import({JpaAuditingConfig.class, ThemeTranslationRepositoryImpl.class, ThemeTranslationMapper.class})
@DisplayName("Persistencia de temas")
class ThemePersistenceTest {

    @Autowired private TestEntityManager entityManager;
    @Autowired private ThemeTranslationRepositoryImpl translations;

    @Test
    @DisplayName("guarda y lee la traducción de un tema en un idioma")
    void readsTranslationByThemeAndLanguage() {
        entityManager.persistAndFlush(ThemeTranslationEntity.builder().themeId(2).languageId(2)
                .translatedName("Dark").translatedDescription("Dark theme")
                .source(TranslationSource.MANUAL).status(TranslationStatus.PUBLISHED).build());

        assertThat(translations.findByThemeAndLanguage(2, 2))
                .hasValueSatisfying(t -> {
                    assertThat(t.getTranslatedName()).isEqualTo("Dark");
                    assertThat(t.getStatus()).isEqualTo(TranslationStatus.PUBLISHED);
                });
        assertThat(translations.findByThemeAndLanguage(2, 3)).isEmpty();
    }
}
