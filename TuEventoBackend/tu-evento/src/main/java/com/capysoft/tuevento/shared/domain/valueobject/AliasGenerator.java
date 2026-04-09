package com.capysoft.tuevento.shared.domain.valueobject;

import java.util.function.Predicate;

/**
 * Genera un alias único a partir de un email o nombre.
 * Si el email es null (ej. OAuth sin permiso de email), usa el alias/nombre como base.
 * Toma la parte local del email (antes del @), elimina caracteres especiales,
 * convierte a minúsculas y agrega sufijo numérico si ya existe.
 */
public final class AliasGenerator {

    private AliasGenerator() {}

    /**
     * Genera un alias base a partir del email o, si es null, del nombre/alias provisto.
     *
     * @param email email del usuario (puede ser null)
     * @param fallbackName nombre a usar si el email es null
     * @return alias base en minúsculas sin caracteres especiales
     */
    public static String generate(String email, String fallbackName) {
        String base = (email != null && email.contains("@"))
                ? email.split("@")[0]
                : (fallbackName != null ? fallbackName : "user");
        return base.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    /**
     * Genera un alias base a partir del email.
     * Si el email es null explota con NPE — usar generate(email, fallback) cuando el email puede ser null.
     *
     * @param email email del usuario
     * @return alias base en minúsculas sin caracteres especiales
     */
    public static String generate(String email) {
        return generate(email, null);
    }

    /**
     * Genera un alias único verificando contra la BD mediante el predicado.
     * Si el alias base ya existe, agrega sufijo numérico incremental.
     *
     * @param email       email del usuario (puede ser null)
     * @param fallback    nombre a usar si el email es null
     * @param existsBy    predicado que retorna true si el alias ya está en uso
     * @return alias único garantizado
     */
    public static String generateUnique(String email, String fallback, Predicate<String> existsBy) {
        String base = generate(email, fallback);
        if (base.isEmpty()) base = "user";
        if (!existsBy.test(base)) {
            return base;
        }
        int suffix = 1;
        String candidate;
        do {
            candidate = base + suffix;
            suffix++;
        } while (existsBy.test(candidate));
        return candidate;
    }

    /**
     * Genera un alias único a partir del email.
     * Si el alias base ya existe, agrega sufijo numérico incremental.
     *
     * @param email     email del usuario
     * @param existsBy  predicado que retorna true si el alias ya está en uso
     * @return alias único garantizado
     */
    public static String generateUnique(String email, Predicate<String> existsBy) {
        return generateUnique(email, null, existsBy);
    }
}
