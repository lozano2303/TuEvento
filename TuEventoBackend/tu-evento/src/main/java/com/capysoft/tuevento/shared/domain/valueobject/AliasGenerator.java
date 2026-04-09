package com.capysoft.tuevento.shared.domain.valueobject;

import java.util.function.Predicate;

/**
 * Genera un alias único a partir de un email.
 * Toma la parte local (antes del @), elimina caracteres especiales,
 * convierte a minúsculas y agrega sufijo numérico si ya existe.
 */
public final class AliasGenerator {

    private AliasGenerator() {}

    /**
     * Genera un alias base a partir del email sin verificar unicidad.
     * Útil cuando no se necesita comprobar duplicados.
     *
     * @param email email del usuario
     * @return alias base en minúsculas sin caracteres especiales
     */
    public static String generate(String email) {
        String localPart = email.split("@")[0];
        return localPart.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    /**
     * Genera un alias único verificando contra la BD mediante el predicado.
     * Si el alias base ya existe, agrega sufijo numérico incremental.
     *
     * @param email     email del usuario
     * @param existsBy  predicado que retorna true si el alias ya está en uso
     * @return alias único garantizado
     */
    public static String generateUnique(String email, Predicate<String> existsBy) {
        String base = generate(email);
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
}
