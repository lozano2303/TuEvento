package com.capysoft.identity.shared.interfaces.translation;

import java.util.Map;

import com.capysoft.identity.shared.domain.valueobject.TranslationSource;
import com.capysoft.identity.shared.domain.valueobject.TranslationStatus;

/**
 * Reglas comunes de las pantallas de administración de traducciones ({@code /api/v1/admin/*-translations}):
 * qué filas muestra cada filtro del listado y cómo se leen {@code source} y {@code status} del cuerpo
 * de un alta o edición. Cada dominio (categorías, tipos de sección…) tiene su controlador y usa estas
 * reglas para que todos se comporten igual.
 */
public final class TranslationAdminSupport {

    /** Filtros del listado: todas, sin traducir, traducidas y pendientes de revisión. */
    public static final String FILTER_ALL = "all";
    public static final String FILTER_NOT_TRANSLATED = "not-translated";
    public static final String FILTER_TRANSLATED = "translated";
    public static final String FILTER_NEEDS_REVIEW = "needs-review";

    private TranslationAdminSupport() {
    }

    /**
     * Resultado de aplicar un filtro a una fila.
     *
     * @param included    la fila aparece en el listado
     * @param needsReview la fila se marca como pendiente de revisión (solo con {@code needs-review})
     */
    public record FilterMatch(boolean included, boolean needsReview) {
    }

    /**
     * Decide si una fila entra en el listado. Una traducción vacía o igual al original (sin
     * distinguir mayúsculas ni espacios) cuenta como pendiente de revisión. Un filtro desconocido
     * no muestra nada.
     *
     * @param original   texto original en español
     * @param translated texto traducido, o {@code null} si no hay traducción
     */
    public static FilterMatch match(String filter, String original, String translated) {
        boolean missing = translated == null || translated.isBlank();
        return switch (filter) {
            case FILTER_ALL -> new FilterMatch(true, false);
            case FILTER_NOT_TRANSLATED -> new FilterMatch(missing, false);
            case FILTER_TRANSLATED -> new FilterMatch(!missing, false);
            case FILTER_NEEDS_REVIEW -> {
                boolean review = missing || original.trim().equalsIgnoreCase(translated.trim());
                yield new FilterMatch(review, review);
            }
            default -> new FilterMatch(false, false);
        };
    }

    /** {@code source} del cuerpo; si falta o no es válido, la traducción es manual. */
    public static TranslationSource sourceOf(Map<String, String> body) {
        return parse(TranslationSource.class, body.get("source"), TranslationSource.MANUAL);
    }

    /** {@code status} del cuerpo; si falta o no es válido, la traducción queda publicada. */
    public static TranslationStatus statusOf(Map<String, String> body) {
        return parse(TranslationStatus.class, body.get("status"), TranslationStatus.PUBLISHED);
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, E fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
