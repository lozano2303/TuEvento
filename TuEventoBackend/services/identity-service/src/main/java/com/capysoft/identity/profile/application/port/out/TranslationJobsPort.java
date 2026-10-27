package com.capysoft.identity.profile.application.port.out;

/**
 * Trabajos de traducción de la biografía del perfil. Los ejecuta el motor de traducciones, que hoy
 * vive en el monolito y escribe el texto traducido en profile_translation (misma base de datos).
 */
public interface TranslationJobsPort {

    /**
     * Estado del trabajo de traducción de un perfil a un idioma. Si no se puede consultar, se
     * responde {@link JobState#FAILED}: el cliente deja de esperar en vez de consultar sin fin.
     */
    JobState stateOf(Long profileId, Integer targetLanguageId);

    /** Pide la traducción de la biografía del perfil desde el idioma de origen. */
    void request(Long profileId, String sourceLanguageCode);

    /** Descarta los trabajos del perfil (su biografía cambió); se aplica en segundo plano. */
    void discard(Long profileId);

    enum JobState { NONE, IN_PROGRESS, COMPLETED, FAILED }
}
