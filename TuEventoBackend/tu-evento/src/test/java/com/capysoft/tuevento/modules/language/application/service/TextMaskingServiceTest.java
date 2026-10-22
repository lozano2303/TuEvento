package com.capysoft.tuevento.modules.language.application.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TextMaskingServiceTest {

    private final TextMaskingService service = new TextMaskingService("Tu Evento,GitHub,Docker");

    @Test
    void testMaskProtectedTerms() {
        String text = "Welcome to Tu Evento! We use GitHub and Docker for development.";
        
        TextMaskingService.MaskingResult result = service.maskText(text);
        
        // Should mask all protected terms
        assertFalse(result.maskedText.contains("Tu Evento"));
        assertFalse(result.maskedText.contains("GitHub"));
        assertFalse(result.maskedText.contains("Docker"));
        
        // Should have term mappings
        assertEquals(3, result.termMappings.size());
        assertTrue(result.termMappings.containsValue("Tu Evento"));
        assertTrue(result.termMappings.containsValue("GitHub"));
        assertTrue(result.termMappings.containsValue("Docker"));
    }

    @Test
    void testMaskVariables() {
        String text = "Hello {name}, welcome to {platform}!";
        
        TextMaskingService.MaskingResult result = service.maskText(text);
        
        // Should mask variables
        assertFalse(result.maskedText.contains("{name}"));
        assertFalse(result.maskedText.contains("{platform}"));
        
        // Should have variable mappings
        assertEquals(2, result.variableMappings.size());
        assertTrue(result.variableMappings.containsValue("{name}"));
        assertTrue(result.variableMappings.containsValue("{platform}"));
    }

    @Test
    void testCaseInsensitiveMatching() {
        String text = "tu evento is great and TU EVENTO rocks!";
        
        TextMaskingService.MaskingResult result = service.maskText(text);
        
        // Should preserve original case in mappings
        assertTrue(result.termMappings.containsValue("tu evento"));
        assertTrue(result.termMappings.containsValue("TU EVENTO"));
    }

    @Test
    void testUnmaskSuccess() throws TextMaskingService.MaskingException {
        String originalText = "Welcome to Tu Evento! User {name} logged in.";
        
        TextMaskingService.MaskingResult maskResult = service.maskText(originalText);
        String translatedMasked = maskResult.maskedText.replace("Welcome", "Bienvenido");
        
        String unmasked = service.unmaskText(translatedMasked, 
                maskResult.termMappings, maskResult.variableMappings);
        
        assertTrue(unmasked.contains("Tu Evento"));
        assertTrue(unmasked.contains("{name}"));
        assertTrue(unmasked.contains("Bienvenido"));
    }

    @Test
    void testUnmaskWithMissingPlaceholder() {
        String originalText = "Welcome to Tu Evento!";
        TextMaskingService.MaskingResult maskResult = service.maskText(originalText);
        
        // Simulate translation that removes a placeholder
        String translatedMasked = "Bienvenido"; // Missing the {{T_1}} placeholder
        
        assertThrows(TextMaskingService.MaskingException.class, () -> {
            service.unmaskText(translatedMasked, maskResult.termMappings, maskResult.variableMappings);
        });
    }

    @Test
    void testEmptyText() {
        TextMaskingService.MaskingResult result = service.maskText("");
        
        assertEquals("", result.maskedText);
        assertTrue(result.termMappings.isEmpty());
        assertTrue(result.variableMappings.isEmpty());
    }

    @Test
    void testNullText() {
        TextMaskingService.MaskingResult result = service.maskText(null);
        
        assertNull(result.maskedText);
        assertTrue(result.termMappings.isEmpty());
        assertTrue(result.variableMappings.isEmpty());
    }

    @Test
    void testLongerTermsFirst() {
        // Test that "Docker Hub" is matched before "Docker" 
        TextMaskingService serviceWithOrder = new TextMaskingService("Docker,Docker Hub");
        String text = "We use Docker Hub and Docker containers.";
        
        TextMaskingService.MaskingResult result = serviceWithOrder.maskText(text);
        
        // Should have mappings for both terms
        assertTrue(result.termMappings.containsValue("Docker Hub"));
        assertTrue(result.termMappings.containsValue("Docker"));
    }

    @Test
    void testCombinedTermAndVariableWithTranslation() throws TextMaskingService.MaskingException {
        String originalText = "Welcome {user} to Tu Evento! Visit {website}.";
        
        TextMaskingService.MaskingResult maskResult = service.maskText(originalText);
        
        // Simulate translation that preserves all placeholders
        String translatedMasked = maskResult.maskedText
                .replace("Welcome", "Bienvenido")
                .replace("to", "a")
                .replace("Visit", "Visita");
        
        String unmasked = service.unmaskText(translatedMasked, 
                maskResult.termMappings, maskResult.variableMappings);
        
        assertTrue(unmasked.contains("Tu Evento"));
        assertTrue(unmasked.contains("{user}"));
        assertTrue(unmasked.contains("{website}"));
        assertTrue(unmasked.contains("Bienvenido"));
    }

    // Tests nuevos con ida y vuelta
    @Test
    void testVariableAtStart() throws TextMaskingService.MaskingException {
        String originalText = "{name} te espera en Tu Evento";
        
        TextMaskingService.MaskingResult maskResult = service.maskText(originalText);
        
        // Assert masked text properties
        assertFalse(maskResult.maskedText.contains("{name}"));
        assertTrue(maskResult.maskedText.contains("{{V_1}}"));
        assertTrue(maskResult.maskedText.contains("{{T_1}}"));
        
        String translatedMasked = maskResult.maskedText.replace("te espera en", "awaits you at");
        String unmasked = service.unmaskText(translatedMasked, 
                maskResult.termMappings, maskResult.variableMappings);
        
        assertEquals(originalText.replace("te espera en", "awaits you at"), unmasked);
    }

    @Test 
    void testVariableAtEnd() throws TextMaskingService.MaskingException {
        String originalText = "Reserva ahora, {name}";
        
        TextMaskingService.MaskingResult maskResult = service.maskText(originalText);
        
        // Assert masked text properties
        assertFalse(maskResult.maskedText.contains("{name}"));
        assertTrue(maskResult.maskedText.contains("{{V_1}}"));
        
        String translatedMasked = maskResult.maskedText.replace("Reserva ahora", "Book now");
        String unmasked = service.unmaskText(translatedMasked, 
                maskResult.termMappings, maskResult.variableMappings);
        
        assertEquals(originalText.replace("Reserva ahora", "Book now"), unmasked);
    }

    @Test
    void testDoubleKey() throws TextMaskingService.MaskingException {
        String originalText = "Hola {{name}}, bienvenido";
        
        TextMaskingService.MaskingResult maskResult = service.maskText(originalText);
        
        // Assert masked text properties
        assertFalse(maskResult.maskedText.contains("{{name}}"));
        assertTrue(maskResult.maskedText.contains("{{V_1}}"));
        
        String translatedMasked = maskResult.maskedText.replace("Hola", "Hello").replace("bienvenido", "welcome");
        String unmasked = service.unmaskText(translatedMasked, 
                maskResult.termMappings, maskResult.variableMappings);
        
        assertEquals(originalText.replace("Hola", "Hello").replace("bienvenido", "welcome"), unmasked);
    }

    @Test
    void testTwoVariablesTogether() throws TextMaskingService.MaskingException {
        String originalText1 = "{first}{last}";
        String originalText2 = "{first} {last}";
        
        // Test case 1: no space
        TextMaskingService.MaskingResult maskResult1 = service.maskText(originalText1);
        assertFalse(maskResult1.maskedText.contains("{first}"));
        assertFalse(maskResult1.maskedText.contains("{last}"));
        assertTrue(maskResult1.maskedText.contains("{{V_1}}"));
        assertTrue(maskResult1.maskedText.contains("{{V_2}}"));
        
        String unmasked1 = service.unmaskText(maskResult1.maskedText, 
                maskResult1.termMappings, maskResult1.variableMappings);
        assertEquals(originalText1, unmasked1);
        
        // Test case 2: with space  
        TextMaskingService.MaskingResult maskResult2 = service.maskText(originalText2);
        assertFalse(maskResult2.maskedText.contains("{first}"));
        assertFalse(maskResult2.maskedText.contains("{last}"));
        assertTrue(maskResult2.maskedText.contains("{{V_1}}"));
        assertTrue(maskResult2.maskedText.contains("{{V_2}}"));
        
        String unmasked2 = service.unmaskText(maskResult2.maskedText,
                maskResult2.termMappings, maskResult2.variableMappings);
        assertEquals(originalText2, unmasked2);
    }

    @Test
    void testVariablePlusProtectedTerm() throws TextMaskingService.MaskingException {
        String originalText = "Hello {user}, welcome to Tu Evento!";
        
        TextMaskingService.MaskingResult maskResult = service.maskText(originalText);
        
        // Assert masked text properties
        assertFalse(maskResult.maskedText.contains("{user}"));
        assertFalse(maskResult.maskedText.contains("Tu Evento"));
        assertTrue(maskResult.maskedText.contains("{{V_1}}"));
        assertTrue(maskResult.maskedText.contains("{{T_1}}"));
        
        String translatedMasked = maskResult.maskedText.replace("Hello", "Hola").replace("welcome to", "bienvenido a");
        String unmasked = service.unmaskText(translatedMasked, 
                maskResult.termMappings, maskResult.variableMappings);
        
        assertEquals(originalText.replace("Hello", "Hola").replace("welcome to", "bienvenido a"), unmasked);
    }

    @Test
    void testMissingPlaceholderAfterTranslation() {
        String originalText = "Welcome to Tu Evento, {name}!";
        TextMaskingService.MaskingResult maskResult = service.maskText(originalText);
        
        // Simulate translation that removes a placeholder
        String translatedMasked = "Bienvenido"; // Missing placeholders
        
        TextMaskingService.MaskingException exception = assertThrows(
                TextMaskingService.MaskingException.class, () -> {
            service.unmaskText(translatedMasked, maskResult.termMappings, maskResult.variableMappings);
        });
        
        assertTrue(exception.getMessage().contains("Missing placeholder in translated text"));
    }

    @Test
    void testTextWithoutVariablesOrTerms() throws TextMaskingService.MaskingException {
        String originalText = "This is a simple text without any special content.";
        
        TextMaskingService.MaskingResult maskResult = service.maskText(originalText);
        
        // Should remain identical
        assertEquals(originalText, maskResult.maskedText);
        assertTrue(maskResult.termMappings.isEmpty());
        assertTrue(maskResult.variableMappings.isEmpty());
        
        String unmasked = service.unmaskText(maskResult.maskedText, 
                maskResult.termMappings, maskResult.variableMappings);
        assertEquals(originalText, unmasked);
    }
}