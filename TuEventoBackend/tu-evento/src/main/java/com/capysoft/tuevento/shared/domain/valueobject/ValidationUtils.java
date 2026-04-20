package com.capysoft.tuevento.shared.domain.valueobject;

import java.util.regex.Pattern;

import com.capysoft.tuevento.shared.domain.exception.BusinessException;

public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@gmail\\.com$");

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$");

    private static final Pattern FULL_NAME_PATTERN =
            Pattern.compile("^[a-zA-Z\u00e1\u00e9\u00ed\u00f3\u00fa\u00c1\u00c9\u00cd\u00d3\u00da\u00f1\u00d1\u00fc\u00dc]{2,}(\\s[a-zA-Z\u00e1\u00e9\u00ed\u00f3\u00fa\u00c1\u00c9\u00cd\u00d3\u00da\u00f1\u00d1\u00fc\u00dc]{2,})+$");

    private ValidationUtils() {}

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
        if (fullName == null || !FULL_NAME_PATTERN.matcher(fullName.trim()).matches()) {
            throw new BusinessException("INVALID_NAME", "Name must contain at least two words with letters only");
        }
    }
}
