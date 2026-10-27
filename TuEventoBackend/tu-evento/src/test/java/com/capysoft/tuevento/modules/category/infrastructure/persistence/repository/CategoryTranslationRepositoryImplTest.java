package com.capysoft.tuevento.modules.category.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.category.domain.model.CategoryTranslation;
import com.capysoft.tuevento.modules.category.infrastructure.persistence.entity.CategoryTranslationEntity;
import com.capysoft.tuevento.modules.category.infrastructure.persistence.mapper.CategoryTranslationMapper;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Adaptador de persistencia de traducciones de categoría con el mapper real y el repositorio JPA simulado.
 */
@DisplayName("CategoryTranslationRepositoryImpl")
class CategoryTranslationRepositoryImplTest {

    private final CategoryTranslationJpaRepository jpa = mock(CategoryTranslationJpaRepository.class);
    private final CategoryTranslationMapper mapper = new CategoryTranslationMapper();
    private final CategoryTranslationRepositoryImpl repository = new CategoryTranslationRepositoryImpl(jpa, mapper);

    private static CategoryTranslationEntity entity(Integer id, String name) {
        return CategoryTranslationEntity.builder().translationId(id).categoryId(1).languageId(2)
                .translatedName(name).translatedDescription("Desc").source(TranslationSource.MACHINE)
                .status(TranslationStatus.PUBLISHED).build();
    }

    private static CategoryTranslation domain(Integer id, String name) {
        return CategoryTranslation.builder().translationId(id).categoryId(1).languageId(2).translatedName(name)
                .translatedDescription("Desc").source(TranslationSource.MANUAL).status(TranslationStatus.DRAFT).build();
    }

    @Test
    @DisplayName("busca por categoría e idioma y convierte a dominio")
    void findsByCategoryAndLanguage() {
        when(jpa.findByCategoryIdAndLanguageId(1, 2)).thenReturn(Optional.of(entity(5, "Music")));

        assertThat(repository.findByCategoryAndLanguage(1, 2)).get()
                .satisfies(t -> {
                    assertThat(t.getTranslationId()).isEqualTo(5);
                    assertThat(t.getTranslatedName()).isEqualTo("Music");
                    assertThat(t.getSource()).isEqualTo(TranslationSource.MACHINE);
                });
    }

    @Test
    @DisplayName("sin id crea una entidad nueva")
    void savesNew() {
        when(jpa.save(any())).thenAnswer(inv -> {
            CategoryTranslationEntity e = inv.getArgument(0);
            e.setTranslationId(9);
            return e;
        });

        CategoryTranslation saved = repository.save(domain(null, "Music"));

        assertThat(saved.getTranslationId()).isEqualTo(9);
        assertThat(saved.getStatus()).isEqualTo(TranslationStatus.DRAFT);
        verify(jpa, never()).findById(any());
    }

    @Test
    @DisplayName("con id actualiza la entidad existente")
    void updatesExisting() {
        CategoryTranslationEntity existing = entity(5, "Old");
        when(jpa.findById(5)).thenReturn(Optional.of(existing));
        when(jpa.save(existing)).thenReturn(existing);

        CategoryTranslation saved = repository.save(domain(5, "Music"));

        assertThat(saved.getTranslatedName()).isEqualTo("Music");
        assertThat(existing.getSource()).isEqualTo(TranslationSource.MANUAL);
        assertThat(existing.getStatus()).isEqualTo(TranslationStatus.DRAFT);
    }

    @Test
    @DisplayName("actualizar un id inexistente falla")
    void updateMissing() {
        when(jpa.findById(5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> repository.save(domain(5, "Music"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("borra por id y no hace nada sin id")
    void deletes() {
        repository.delete(domain(5, "Music"));
        repository.delete(domain(null, "Music"));

        verify(jpa).deleteById(5);
        verify(jpa, never()).deleteById(null);
    }

    @Test
    @DisplayName("el mapper tolera nulos y una entidad nueva arranca en borrador")
    void mapperNulls() {
        assertThat(mapper.toDomain(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();
        mapper.updateEntity(null, domain(1, "x"));
        assertThat(new CategoryTranslationEntity().getStatus()).isEqualTo(TranslationStatus.DRAFT);
        assertThat(CategoryTranslationEntity.builder().build().getStatus()).isEqualTo(TranslationStatus.DRAFT);
    }
}
