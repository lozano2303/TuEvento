package com.capysoft.tuevento.modules.theme.application.usecase;

import com.capysoft.tuevento.modules.theme.application.dto.response.ResolvedPaletteResponse;
import com.capysoft.tuevento.modules.theme.application.port.in.GetActivePalettePort;
import com.capysoft.tuevento.modules.theme.domain.model.Theme;
import com.capysoft.tuevento.modules.theme.domain.model.ThemeCustomization;
import com.capysoft.tuevento.modules.theme.domain.model.UserTheme;
import com.capysoft.tuevento.modules.theme.domain.repository.ThemeCustomizationRepository;
import com.capysoft.tuevento.modules.theme.domain.repository.ThemeRepository;
import com.capysoft.tuevento.modules.theme.domain.repository.UserThemeRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GetActivePaletteUseCase implements GetActivePalettePort {

    private static final String DEFAULT_THEME_NAME = "DARK";

    private final ThemeRepository              themeRepository;
    private final UserThemeRepository          userThemeRepository;
    private final ThemeCustomizationRepository customizationRepository;
    private final ThemePaletteResolver         paletteResolver;
    private final ActivateThemePort            activateThemePort;

    @Override
    @Transactional
    public ResolvedPaletteResponse getActivePalette(Integer userId) {
        return userThemeRepository.findActiveByUserId(userId)
                .map(userTheme -> buildResponse(userTheme))
                .orElseGet(() -> activateDefaultTheme(userId));
    }

    private ResolvedPaletteResponse buildResponse(UserTheme userTheme) {
        Theme theme = themeRepository.findById(userTheme.getThemeId())
                .orElseThrow(() -> new NotFoundException("THEME_NOT_FOUND",
                        "Theme not found with id: " + userTheme.getThemeId()));

        List<ThemeCustomization> customizations =
                customizationRepository.findByUserThemeId(userTheme.getId());
        Map<String, Object> palette = paletteResolver.resolve(theme, customizations);

        return ResolvedPaletteResponse.builder()
                .themeId(theme.getId())
                .themeName(theme.getName())
                .userThemeId(userTheme.getId())
                .palette(palette)
                .build();
    }

    private ResolvedPaletteResponse activateDefaultTheme(Integer userId) {
        Theme defaultTheme = themeRepository.findByName(DEFAULT_THEME_NAME)
                .orElseThrow(() -> new NotFoundException("DEFAULT_THEME_NOT_FOUND",
                        "Default theme '" + DEFAULT_THEME_NAME + "' not found"));
        return activateThemePort.activate(userId, defaultTheme.getId());
    }
}
