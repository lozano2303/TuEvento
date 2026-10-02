package com.capysoft.tuevento.modules.language.domain.model;

import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Agregado raíz Language.
 * Representa un idioma del sistema con configuración de activación y por defecto.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Language {

    private Long languageId;
    private String code;
    private String name;
    private Boolean isActive;
    private Boolean isDefault;

    /**
     * Activa el idioma.
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * Desactiva el idioma.
     * No se puede desactivar el idioma por defecto.
     */
    public void deactivate() {
        if (Boolean.TRUE.equals(this.isDefault)) {
            throw new BusinessException("CANNOT_DEACTIVATE_DEFAULT_LANGUAGE", "Cannot deactivate the default language");
        }
        this.isActive = false;
    }

    /**
     * Marca este idioma como el por defecto.
     */
    public void setAsDefault() {
        this.isDefault = true;
        this.isActive = true; // El idioma por defecto debe estar activo
    }

    /**
     * Remueve la marca de por defecto.
     */
    public void unsetAsDefault() {
        this.isDefault = false;
    }
}