package com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper;

import com.capysoft.tuevento.modules.language.domain.model.TranslationJob;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.TranslationJobEntity;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El texto original del trabajo se guarda como JSON y se recupera igual. */
@DisplayName("TranslationJobMapper — texto original")
class TranslationJobMapperTest {

    private final TranslationJobMapper mapper = new TranslationJobMapper();

    @Test
    @DisplayName("ida y vuelta: el texto se conserva, con acentos y comillas")
    void roundTrip() {
        Map<String, String> texts = Map.of("event_name", "Concierto \"Año nuevo\"", "description", "Música en vivo");
        TranslationJob job = TranslationJob.builder().entityType("event").entityId(7)
                .sourceHash("h").sourceTexts(texts).status(TranslationJobStatus.PENDING).build();

        TranslationJobEntity entity = mapper.toEntity(job);

        assertThat(entity.getSourceTexts()).contains("event_name");
        assertThat(mapper.toDomain(entity).getSourceTexts()).isEqualTo(texts);
    }

    @Test
    @DisplayName("un trabajo antiguo sin texto se lee sin texto; un JSON roto es un error")
    void missingAndBrokenTexts() {
        assertThat(mapper.toEntity(TranslationJob.builder().build()).getSourceTexts()).isNull();
        assertThat(mapper.toDomain(TranslationJobEntity.builder().build()).getSourceTexts()).isNull();

        TranslationJobEntity broken = TranslationJobEntity.builder().sourceTexts("{no es json").build();
        assertThatThrownBy(() -> mapper.toDomain(broken)).isInstanceOf(IllegalStateException.class);
    }
}
