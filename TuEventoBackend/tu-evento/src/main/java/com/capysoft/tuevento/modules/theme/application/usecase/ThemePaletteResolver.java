package com.capysoft.tuevento.modules.theme.application.usecase;

import com.capysoft.tuevento.modules.theme.domain.model.Theme;
import com.capysoft.tuevento.modules.theme.domain.model.ThemeCustomization;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the final palette for a user by merging the theme's defaultPalette
 * with the user's customizations. Customizations override base palette keys.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThemePaletteResolver {

    private final ObjectMapper objectMapper;

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    /**
     * Parses {@code theme.getDefaultPalette()} as {@code Map<String, Object>},
     * then overlays each customization on top.
     *
     * @param theme          the base theme containing the JSON palette
     * @param customizations user-specific overrides
     * @return resolved palette map
     */
    public Map<String, Object> resolve(Theme theme, List<ThemeCustomization> customizations) {
        Map<String, Object> palette = parseDefaultPalette(theme.getDefaultPalette());

        for (ThemeCustomization c : customizations) {
            palette.put(c.getProperty(), c.getValue());
        }

        return palette;
    }

    private Map<String, Object> parseDefaultPalette(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return new HashMap<>(objectMapper.readValue(json, MAP_TYPE));
        } catch (Exception e) {
            log.warn("Failed to parse defaultPalette JSON, returning empty palette. Error: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}
