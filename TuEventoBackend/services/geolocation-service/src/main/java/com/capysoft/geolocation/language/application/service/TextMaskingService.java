package com.capysoft.geolocation.language.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio para enmascarar términos protegidos y variables en textos.
 */
@Service
@Slf4j
public class TextMaskingService {

    private final List<String> protectedTerms;
    private final Pattern variablePattern;
    
    public TextMaskingService(@Value("${translation.protected-terms:Tu Evento}") String protectedTermsConfig) {
        // Parse protected terms from configuration
        this.protectedTerms = parseProtectedTerms(protectedTermsConfig);
        // Match both {{name}} and {name} - double brace first in alternation
        this.variablePattern = Pattern.compile("\\{\\{[^{}]+\\}\\}|\\{[^{}]+\\}");
        
        log.info("Initialized TextMaskingService with protected terms: {}", this.protectedTerms);
    }

    private List<String> parseProtectedTerms(String config) {
        if (config == null || config.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> terms = new ArrayList<>();
        for (String term : config.split(",")) {
            String cleaned = term.trim();
            if (!cleaned.isEmpty()) {
                terms.add(cleaned);
            }
        }
        
        // Sort by length descending (longer terms first for better matching)
        terms.sort((a, b) -> Integer.compare(b.length(), a.length()));
        
        return terms;
    }

    /**
     * Resultado del enmascarado con mapas de restauración.
     */
    public static class MaskingResult {
        public final String maskedText;
        public final Map<String, String> termMappings; // {{T_n}} -> original term
        public final Map<String, String> variableMappings; // {{V_n}} -> original variable
        
        public MaskingResult(String maskedText, Map<String, String> termMappings, Map<String, String> variableMappings) {
            this.maskedText = maskedText;
            this.termMappings = termMappings;
            this.variableMappings = variableMappings;
        }
    }

    /**
     * Enmascara términos protegidos y variables en el texto.
     */
    public MaskingResult maskText(String text) {
        if (text == null || text.isEmpty()) {
            return new MaskingResult(text, Collections.emptyMap(), Collections.emptyMap());
        }

        Map<String, String> termMappings = new HashMap<>();
        Map<String, String> variableMappings = new HashMap<>();
        String result = text;

        // Mask variables first {name}
        int variableCounter = 1;
        Matcher variableMatcher = variablePattern.matcher(result);
        while (variableMatcher.find()) {
            String originalVariable = variableMatcher.group();
            String placeholder = "{{V_" + variableCounter + "}}";
            variableMappings.put(placeholder, originalVariable);
            result = result.replace(originalVariable, placeholder);
            variableCounter++;
        }

        // Mask protected terms after (longer terms first)
        int termCounter = 1;
        for (String term : protectedTerms) {
            // Coincidencia exacta (sensible a mayúsculas): la marca "Tu Evento" no debe
            // atrapar la frase común "tu evento" / "Tu evento" ("your event"), que sí se traduce.
            Pattern termPattern = Pattern.compile("\\b" + Pattern.quote(term) + "\\b");
            Matcher matcher = termPattern.matcher(result);
            
            while (matcher.find()) {
                String originalMatch = matcher.group();
                String placeholder = "{{T_" + termCounter + "}}";
                termMappings.put(placeholder, originalMatch);
                result = result.replace(originalMatch, placeholder);
                termCounter++;
            }
        }

        log.debug("Masked text: {} terms, {} variables", termMappings.size(), variableMappings.size());
        return new MaskingResult(result, termMappings, variableMappings);
    }

    /**
     * Restaura los términos enmascarados en el texto traducido.
     * 
     * @throws MaskingException Si faltan marcadores en el texto traducido
     */
    public String unmaskText(String translatedText, Map<String, String> termMappings, Map<String, String> variableMappings) 
            throws MaskingException {
        if (translatedText == null) {
            return null;
        }

        String result = translatedText;

        // Restore in INVERSE order: terms first, then variables
        for (Map.Entry<String, String> entry : termMappings.entrySet()) {
            result = restorePlaceholder(result, entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : variableMappings.entrySet()) {
            result = restorePlaceholder(result, entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * Reemplaza un marcador por su valor original tolerando las deformaciones que
     * introduce el motor de traducción: LibreTranslate devuelve {{T_1}} como
     * "{{T _ 1}", "{{T   1}" o "{{T 1}}" según el idioma destino.
     *
     * @throws MaskingException si el marcador no aparece en ninguna variante
     */
    private String restorePlaceholder(String text, String placeholder, String original) throws MaskingException {
        Matcher parts = PLACEHOLDER_PARTS.matcher(placeholder);
        if (!parts.matches()) {
            throw new MaskingException("Invalid placeholder format: " + placeholder);
        }
        // {+ espacios LETRA [espacios/_]* NÚMERO (sin más dígitos) espacios }*
        Pattern tolerant = Pattern.compile(
                "\\{+\\s*" + parts.group(1) + "[\\s_]*" + parts.group(2) + "(?!\\d)\\s*\\}*",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = tolerant.matcher(text);
        if (!matcher.find()) {
            throw new MaskingException("Missing placeholder in translated text: " + placeholder);
        }
        return matcher.replaceAll(Matcher.quoteReplacement(original));
    }

    /** Descompone un marcador {{T_1}} / {{V_2}} en su letra y su número. */
    private static final Pattern PLACEHOLDER_PARTS = Pattern.compile("\\{\\{([TV])_(\\d+)\\}\\}");

    /**
     * Excepción lanzada cuando falta un marcador en el texto traducido.
     */
    public static class MaskingException extends Exception {
        public MaskingException(String message) {
            super(message);
        }
    }
}