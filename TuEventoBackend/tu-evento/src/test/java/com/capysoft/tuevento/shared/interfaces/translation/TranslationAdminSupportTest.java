package com.capysoft.tuevento.shared.interfaces.translation;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import com.capysoft.tuevento.shared.interfaces.translation.TranslationAdminSupport.FilterMatch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TranslationAdminSupport")
class TranslationAdminSupportTest {

    @ParameterizedTest(name = "filtro {0}, original \"{1}\", traducido \"{2}\" → incluida {3}, revisar {4}")
    @CsvSource(nullValues = "NULL", value = {
            "all,            Concierto, NULL,       true,  false",
            "all,            Concierto, Concert,    true,  false",
            "not-translated, Concierto, NULL,       true,  false",
            "not-translated, Concierto, '  ',       true,  false",
            "not-translated, Concierto, Concert,    false, false",
            "translated,     Concierto, Concert,    true,  false",
            "translated,     Concierto, NULL,       false, false",
            "needs-review,   Concierto, NULL,       true,  true",
            "needs-review,   Concierto, ' concierto ', true, true",
            "needs-review,   Concierto, Concert,    false, false",
            "otro,           Concierto, Concert,    false, false"
    })
    @DisplayName("cada filtro del listado")
    void match(String filter, String original, String translated, boolean included, boolean needsReview) {
        assertThat(TranslationAdminSupport.match(filter, original, translated))
                .isEqualTo(new FilterMatch(included, needsReview));
    }

    @Test
    @DisplayName("source y status del cuerpo, con MANUAL y PUBLISHED si faltan o no son válidos")
    void sourceAndStatus() {
        Map<String, String> valid = Map.of("source", "MACHINE", "status", "DRAFT");
        assertThat(TranslationAdminSupport.sourceOf(valid)).isEqualTo(TranslationSource.MACHINE);
        assertThat(TranslationAdminSupport.statusOf(valid)).isEqualTo(TranslationStatus.DRAFT);

        Map<String, String> invalid = Map.of("source", "robot", "status", "listo");
        assertThat(TranslationAdminSupport.sourceOf(invalid)).isEqualTo(TranslationSource.MANUAL);
        assertThat(TranslationAdminSupport.statusOf(invalid)).isEqualTo(TranslationStatus.PUBLISHED);

        assertThat(TranslationAdminSupport.sourceOf(Map.of())).isEqualTo(TranslationSource.MANUAL);
        assertThat(TranslationAdminSupport.statusOf(Map.of())).isEqualTo(TranslationStatus.PUBLISHED);
    }
}
