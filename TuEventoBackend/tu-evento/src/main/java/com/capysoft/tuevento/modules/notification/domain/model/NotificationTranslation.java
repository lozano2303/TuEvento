package com.capysoft.tuevento.modules.notification.domain.model;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Traducción del asunto y el cuerpo de una notificación a un idioma.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTranslation {

    private Integer translationId;
    private Integer notificationId;
    private Integer languageId;
    private String translatedSubject;
    private String translatedBody;
    private TranslationSource source;
    private TranslationStatus status;
}
