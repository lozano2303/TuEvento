package com.capysoft.tuevento.shared.domain.valueobject;

import java.text.Normalizer;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.capysoft.tuevento.shared.domain.exception.BusinessException;

public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@gmail\\.com$");

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$");

    // Mínimo 2 palabras, cada una con al menos 3 letras (tildes y ñ incluidas).
    // Igual a la validación usada en el registro (CompleteProfile/RegisterScreen).
    private static final Pattern FULL_NAME_PATTERN =
            Pattern.compile("^\\p{L}{3,}(\\s+\\p{L}{3,})+$");

    // ── Forbidden words (bio) ─────────────────────────────────────────────────
    // Loaded once from resources/validation/forbidden-words-es.txt at class init.
    // To add/remove words: edit that file, no code change required.
    private static final Set<String> FORBIDDEN_WORDS = loadForbiddenWords();

    private static Set<String> loadForbiddenWords() {
        try (InputStream is = ValidationUtils.class.getClassLoader()
                .getResourceAsStream("validation/forbidden-words-es.txt")) {
            if (is == null) return Set.of();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(l -> !l.isBlank() && !l.startsWith("#"))
                        .collect(Collectors.toUnmodifiableSet());
            }
        } catch (Exception e) {
            // Fail open — log-worthy in production but should not break the app.
            return Set.of();
        }
    }

    private ValidationUtils() {}

    // ── Public validators ─────────────────────────────────────────────────────

    public static void validateGmailEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new BusinessException("INVALID_EMAIL", "Only @gmail.com emails are accepted");
        }
    }

    public static void validateStrongPassword(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException("INVALID_PASSWORD",
                    "Password must be at least 8 characters and include uppercase, lowercase, number and special character");
        }
    }

    public static void validateFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessException("INVALID_NAME", "El nombre completo es obligatorio.");
        }
        if (fullName.trim().length() > 30) {
            throw new BusinessException("INVALID_NAME", "El nombre no puede superar los 30 caracteres.");
        }
        String[] words = fullName.trim().split("\\s+");
        if (words.length < 2) {
            throw new BusinessException("INVALID_NAME", "Ingresa nombre y apellido.");
        }
        if (!FULL_NAME_PATTERN.matcher(fullName.trim()).matches()) {
            throw new BusinessException("INVALID_NAME",
                    "El nombre solo puede contener letras (mínimo 3 por palabra) y espacios, sin números ni símbolos.");
        }
    }

    /**
     * Validates bio text against the forbidden-words list.
     * Normalisation steps before comparing:
     *   1. Lowercase
     *   2. Strip diacritics (tildes, etc.)
     *   3. Common leetspeak substitutions: 0→o, 1→i, 3→e, 4→a, 5→s, @→a
     *   4. Remove separators between letters (hyphens, dots, underscores)
     * Comparison is WHOLE-WORD (split by whitespace), so "tontería" does NOT
     * match "tonto". Change to substring if stricter filtering is needed.
     *
     * @throws BusinessException("BIO_INAPPROPRIATE_LANGUAGE") on detection.
     */
    public static void validateBio(String bio) {
        if (bio == null || bio.isBlank()) return; // empty bio is fine

        String normalised = normaliseBio(bio);
        for (String word : normalised.split("\\s+")) {
            if (FORBIDDEN_WORDS.contains(word)) {
                throw new BusinessException(
                        "BIO_INAPPROPRIATE_LANGUAGE",
                        "La biografía contiene lenguaje inapropiado.");
            }
        }
    }

    /**
     * Returns true if the given fullName satisfies the FULL_NAME_PATTERN, false otherwise.
     * Unlike {@link #validateFullName}, this never throws — use it for conditional checks
     * (e.g. deciding whether an OAuth-sourced name is usable or requires onboarding).
     */
    public static boolean isValidFullName(String fullName) {
        return fullName != null && FULL_NAME_PATTERN.matcher(fullName.trim()).matches();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Normalises a bio string for forbidden-word comparison.
     * Steps (order matters):
     *   1. Lowercase
     *   2. Strip diacritics (á→a, é→e, í→i, ó→o, ú→u, ñ→n after NFD)
     *   3. Multi-char phonetic substitutions (ph→f)
     *   4. Single-char leetspeak + phonetic: 0→o 1→i 3→e 4→a 5→s @→a
     *                                         k→c  z→s  w→v  y→i
     *   5. Remove inline separators between letters (t-o-n-t-o → tonto,
     *      t.o.n.t.o → tonto, t o n t o → tonto)
     *   6. Collapse consecutive duplicate letters (tooontooo → tonto)
     */
    private static String normaliseBio(String text) {
        String s = text.toLowerCase();

        // 2. Strip diacritics
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
                      .replaceAll("\\p{InCombiningDiacriticalMarks}", "");

        // 3. Multi-char phonetic
        s = s.replace("ph", "f")
             .replace("ck", "c");

        // 4. Single-char leetspeak + phonetic
        s = s.replace('0', 'o')
             .replace('1', 'i')
             .replace('3', 'e')
             .replace('4', 'a')
             .replace('5', 's')
             .replace('@', 'a')
             .replace('k', 'c')
             .replace('z', 's')
             .replace('w', 'v')
             .replace('y', 'i');

        // 5a. Remove inline non-space separators between word chars (t-o-n-t-o → tonto)
        s = s.replaceAll("(?<=\\w)[.\\-_](?=\\w)", "");

        // 5b. Collapse single letters separated by spaces into one token
        //     Matches sequences like "t o n t o" → "tonto"
        //     Pattern: non-space char, space, non-space char repeated
        s = s.replaceAll("(?<=\\b)(\\S) (?=\\S ?(\\S )*)(?=[a-z])", "$1");
        // Simpler, more reliable approach: collapse isolated single-char tokens
        s = s.replaceAll("(?<![a-z])([a-z]) (?=[a-z] )", "$1");
        s = s.replaceAll("(?<![a-z])([a-z]) (?=[a-z]\\b)", "$1");

        // 6. Collapse consecutive duplicate letters (aaaa → a, tooontooo → tonto)
        s = s.replaceAll("(.)\\1+", "$1");

        return s;
    }
}
