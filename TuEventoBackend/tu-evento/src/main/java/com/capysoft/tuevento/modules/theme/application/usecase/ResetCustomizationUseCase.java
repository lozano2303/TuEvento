package com.capysoft.tuevento.modules.theme.application.usecase;

import com.capysoft.tuevento.modules.theme.application.dto.response.ResolvedPaletteResponse;
import com.capysoft.tuevento.modules.theme.application.port.in.ResetCustomizationPort;
import com.capysoft.tuevento.modules.theme.domain.event.ThemeCustomizationResetEvent;
import com.capysoft.tuevento.modules.theme.domain.model.Theme;
import com.capysoft.tuevento.modules.theme.domain.model.ThemeCustomization;
import com.capysoft.tuevento.modules.theme.domain.model.ThemeLog;
import com.capysoft.tuevento.modules.theme.domain.model.UserTheme;
import com.capysoft.tuevento.modules.theme.domain.repository.ThemeCustomizationRepository;
import com.capysoft.tuevento.modules.theme.domain.repository.ThemeLogRepository;
import com.capysoft.tuevento.modules.theme.domain.repository.ThemeRepository;
import com.capysoft.tuevento.modules.theme.domain.repository.UserThemeRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResetCustomizationUseCase implements ResetCustomizationPort {

    private final UserThemeRepository          userThemeRepository;
    private final ThemeRepository              themeRepository;
    private final ThemeCustomizationRepository customizationRepository;
    private final ThemeLogRepository           themeLogRepository;
    private final ThemePaletteResolver         paletteResolver;
    private final ApplicationEventPublisher    eventPublisher;

    @Override
    @Transactional
    public ResolvedPaletteResponse reset(Integer userId, String property) {
        UserTheme userTheme = userThemeRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new NotFoundException("ACTIVE_USER_THEME_NOT_FOUND",
                        "No active theme found for user: " + userId));

        // Capture old value before deletion (may be absent if never customized)
        String oldValue = customizationRepository
                .findByUserThemeIdAndProperty(userTheme.getId(), property)
                .map(ThemeCustomization::getValue)
                .orElse(null);

        customizationRepository.deleteByUserThemeIdAndProperty(userTheme.getId(), property);

        // Audit log — newValue is null on reset
        themeLogRepository.save(ThemeLog.builder()
                .userThemeId(userTheme.getId())
                .action("RESET")
                .property(property)
                .oldValue(oldValue)
                .newValue(null)
                .createdAt(LocalDateTime.now())
                .build());

        eventPublisher.publishEvent(ThemeCustomizationResetEvent.builder()
                .userId(userId)
                .userThemeId(userTheme.getId())
                .property(property)
                .build());

        return buildResponse(userTheme);
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
}
