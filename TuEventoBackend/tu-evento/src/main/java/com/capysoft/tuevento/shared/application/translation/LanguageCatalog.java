package com.capysoft.tuevento.shared.application.translation;

import java.util.Optional;

/**
 * Catálogo de idiomas tal como lo ven los dominios: traducir el código que pide el cliente
 * ("en", "fr"…) al id con el que se guardan las traducciones, y al revés. El catálogo lo
 * administra el núcleo de traducción, que puede vivir en otro servicio.
 */
public interface LanguageCatalog {

    Optional<LanguageSummary> findByCode(String code);

    Optional<LanguageSummary> findById(Integer languageId);
}
