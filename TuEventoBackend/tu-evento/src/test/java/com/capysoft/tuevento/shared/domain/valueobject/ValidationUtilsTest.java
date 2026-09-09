package com.capysoft.tuevento.shared.domain.valueobject;

import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ValidationUtils.isValidFullName() and validateFullName().
 *
 * Covers the exact cases relevant to the Google OAuth onboarding bug:
 * - Email-prefix strings with digits must be rejected
 * - Real two-word Spanish names must pass
 * - Single-word inputs must be rejected
 * - null and blank inputs must be rejected
 */
@DisplayName("ValidationUtils — FULL_NAME_PATTERN")
class ValidationUtilsTest {

    // ── isValidFullName — should return false ─────────────────────────────

    @ParameterizedTest(name = "isValidFullName(\"{0}\") = false")
    @ValueSource(strings = {
        "crislozanoshark2006",   // email prefix with digits — the original bug
        "user123",               // digits present
        "abc",                   // single word, no space
        "a b",                   // words too short (< 2 chars each)
        "Jean-Pierre Dupont",    // hyphen not in pattern
        "O'Brien Smith",         // apostrophe not in pattern
        "María",                 // single word only
        "",                      // empty string
    })
    @DisplayName("invalid names are rejected")
    void invalidNames(String input) {
        assertThat(ValidationUtils.isValidFullName(input)).isFalse();
    }

    @Test
    @DisplayName("null input returns false without throwing")
    void nullReturnsFalse() {
        assertThat(ValidationUtils.isValidFullName(null)).isFalse();
    }

    // ── isValidFullName — should return true ──────────────────────────────

    @ParameterizedTest(name = "isValidFullName(\"{0}\") = true")
    @ValueSource(strings = {
        "Cristian Lozano",       // two words, ASCII letters
        "María García",          // accented vowels
        "José Martínez López",   // three words
        "Juan Pablo",            // compound first name
        "Ñoño Ñoño",             // ñ character
        "Üwe Müller",            // ü character
    })
    @DisplayName("valid Spanish names are accepted")
    void validNames(String input) {
        assertThat(ValidationUtils.isValidFullName(input)).isTrue();
    }

    @Test
    @DisplayName("leading/trailing whitespace is trimmed before validation")
    void trimmingWhitespace() {
        assertThat(ValidationUtils.isValidFullName("  Cristian Lozano  ")).isTrue();
    }

    // ── validateFullName — throwing variant ───────────────────────────────

    @Test
    @DisplayName("validateFullName throws BusinessException(INVALID_NAME) for email prefix with digits")
    void throwsForEmailPrefix() {
        assertThatThrownBy(() -> ValidationUtils.validateFullName("crislozanoshark2006"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("INVALID_NAME"));
    }

    @Test
    @DisplayName("validateFullName does not throw for a valid name")
    void doesNotThrowForValidName() {
        // Should complete without exception
        ValidationUtils.validateFullName("Cristian Lozano");
    }

    @Test
    @DisplayName("validateFullName throws for null")
    void throwsForNull() {
        assertThatThrownBy(() -> ValidationUtils.validateFullName(null))
                .isInstanceOf(BusinessException.class);
    }
}
