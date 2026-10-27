package com.capysoft.tuevento.modules.theme.application.usecase;

import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.theme.domain.model.ThemeTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.modules.theme.domain.repository.ThemeTranslationRepository;
import com.capysoft.tuevento.modules.theme.application.dto.response.ThemeResponse;
import com.capysoft.tuevento.modules.theme.application.mapper.ThemeAppMapper;
import com.capysoft.tuevento.modules.theme.application.port.in.GetThemesPort;
import com.capysoft.tuevento.modules.theme.domain.model.Theme;
import com.capysoft.tuevento.modules.theme.domain.repository.ThemeRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetThemesUseCase implements GetThemesPort {

    private final ThemeRepository themeRepository;
    private final ThemeAppMapper mapper;
    private final LanguageRepository languageRepository;
    private final ThemeTranslationRepository themeTranslationRepository;

    @Override
    public List<ThemeResponse> getAll() {
        return getAll(null);
    }

    @Override
    public List<ThemeResponse> getAll(String languageCode) {
        List<Theme> themes = themeRepository.findAll();
        
        // If no language specified or Spanish (source language), return without translations
        if (languageCode == null || languageCode.isBlank() || "es".equalsIgnoreCase(languageCode)) {
            return themes.stream()
                    .map(mapper::toThemeResponse)
                    .toList();
        }
        
        // Find target language
        Optional<Language> language = languageRepository.findByCode(languageCode.toLowerCase());
        if (language.isEmpty()) {
            log.warn("Language not found: {}, returning themes without translations", languageCode);
            return themes.stream()
                    .map(mapper::toThemeResponse)
                    .toList();
        }
        
        Integer languageId = language.get().getLanguageId();
        
        // Map themes with translations
        return themes.stream()
                .map(theme -> toResponseWithTranslation(theme, languageId, languageCode))
                .toList();
    }
    
    private ThemeResponse toResponseWithTranslation(Theme theme, Integer languageId, String languageCode) {
        ThemeResponse.ThemeResponseBuilder builder = ThemeResponse.builder()
                .id(theme.getId())
                .name(theme.getName())
                .description(theme.getDescription());
        
        try {
            Optional<ThemeTranslation> translation = themeTranslationRepository
                    .findByThemeAndLanguage(theme.getId(), languageId);
            
            // Only use PUBLISHED translations
            if (translation.isPresent() && translation.get().getStatus() == TranslationStatus.PUBLISHED) {
                builder.translatedName(translation.get().getTranslatedName())
                       .translatedDescription(translation.get().getTranslatedDescription())
                       .languageCode(languageCode);
            }
        } catch (Exception ex) {
            log.error("Error resolving translation for theme {} language {}: {}", 
                    theme.getId(), languageCode, ex.getMessage());
        }
        
        return builder.build();
    }
}
