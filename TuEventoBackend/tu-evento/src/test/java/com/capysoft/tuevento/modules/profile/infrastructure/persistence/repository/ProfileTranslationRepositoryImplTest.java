package com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.profile.domain.model.ProfileTranslation;
import com.capysoft.tuevento.modules.profile.infrastructure.persistence.entity.ProfileTranslationEntity;
import com.capysoft.tuevento.modules.profile.infrastructure.persistence.mapper.ProfileTranslationMapper;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Adaptador de persistencia de traducciones de perfiles con el mapper real y el repositorio JPA simulado.
 */
@DisplayName("ProfileTranslationRepositoryImpl")
class ProfileTranslationRepositoryImplTest {

    private final ProfileTranslationJpaRepository jpa = mock(ProfileTranslationJpaRepository.class);
    private final ProfileTranslationMapper mapper = new ProfileTranslationMapper();
    private final ProfileTranslationRepositoryImpl repository = new ProfileTranslationRepositoryImpl(jpa, mapper);

    private static ProfileTranslationEntity entity(int profileId, int languageId) {
        return ProfileTranslationEntity.builder().translationId(profileId * 10).profileId(profileId)
                .languageId(languageId).translatedBio("Hello").source(TranslationSource.MACHINE)
                .status(TranslationStatus.PUBLISHED).build();
    }

    @Test
    @DisplayName("busca por perfil e idioma y por idioma")
    void finds() {
        when(jpa.findByProfileIdAndLanguageId(4, 2)).thenReturn(Optional.of(entity(4, 2)));
        when(jpa.findByLanguageId(2)).thenReturn(List.of(entity(4, 2), entity(5, 2)));

        ProfileTranslation found = repository.findByProfileAndLanguage(4L, 2).orElseThrow();
        assertThat(found.getTranslatedBio()).isEqualTo("Hello");
        assertThat(found.getSource()).isEqualTo(TranslationSource.MACHINE);
        assertThat(found.getStatus()).isEqualTo(TranslationStatus.PUBLISHED);
        assertThat(repository.findByLanguage(2)).extracting(ProfileTranslation::getProfileId).containsExactly(4, 5);
    }

    @Test
    @DisplayName("guarda, borra una y borra todas las de un perfil")
    void writes() {
        when(jpa.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ProfileTranslation translation = mapper.toDomain(entity(4, 2));
        translation.setTranslatedBio("Hi");

        assertThat(repository.save(translation).getTranslatedBio()).isEqualTo("Hi");
        repository.delete(translation);
        verify(jpa).delete(any(ProfileTranslationEntity.class));

        List<ProfileTranslationEntity> all = List.of(entity(4, 2), entity(4, 3));
        when(jpa.findByProfileId(4)).thenReturn(all);
        repository.deleteByProfile(4L);
        verify(jpa).deleteAll(all);
    }

    @Test
    @DisplayName("el mapper tolera nulos y la igualdad es por perfil e idioma")
    void mapperAndEquality() {
        assertThat(mapper.toDomain(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();

        ProfileTranslation a = mapper.toDomain(entity(4, 2));
        ProfileTranslation b = mapper.toDomain(entity(4, 2));
        b.setTranslatedBio("otro texto");
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(mapper.toDomain(entity(4, 3)));
    }
}
