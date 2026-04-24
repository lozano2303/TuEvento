package com.capysoft.tuevento.modules.theme.application.usecase;

import com.capysoft.tuevento.modules.theme.application.dto.request.CustomizeThemeRequest;
import com.capysoft.tuevento.modules.theme.application.dto.response.ResolvedPaletteResponse;
import com.capysoft.tuevento.modules.theme.application.port.in.CustomizeThemePort;
import com.capysoft.tuevento.modules.theme.domain.event.ThemeCustomizedEvent;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomizeThemeUseCase implements CustomizeThemePort {

    private final UserThemeRepository          userThemeRepository;
    private final ThemeRepository              themeRepository;
    private final ThemeCustomizationRepository customizationRepository;
    private final ThemeLogRepository           themeLogRepository;
    private final ThemePaletteResolver         paletteResolver;
    private final ApplicationEventPublisher    eventPublisher;

    @Override
    @Transactional
    public ResolvedPaletteResponse customize(Integer userId, CustomizeThemeRequest request) {
        UserTheme userTheme = userThemeRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new NotFoundException("ACTIVE_USER_THEME_NOT_FOUND",
                        "No active theme found for user: " + userId));

        String property = request.getProperty();
        String newValue = request.getValue();

        // Upsert customization
        Optional<ThemeCustomization> existing =
                customizationRepository.findByUserThemeIdAndProperty(userTheme.getId(), property);

        String oldValue = existing.map(ThemeCustomization::getValue).orElse(null);

        ThemeCustomization customization = existing
                .map(c -> {
                    c.setValue(newValue);
                    c.setUpdatedAt(LocalDateTime.now());
                    return c;
                })
                .orElseGet(() -> ThemeCustomization.builder()
                        .userThemeId(userTheme.getId())
                        .property(property)
                        .value(newValue)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());

        customizationRepository.save(customization);

        // Audit log
        themeLogRepository.save(ThemeLog.builder()
                .userThemeId(userTheme.getId())
                .action("UPDATE")
                .property(property)
                .oldValue(oldValue)
                .newValue(newValue)
                .createdAt(LocalDateTime.now())
                .build());

        eventPublisher.publishEvent(ThemeCustomizedEvent.builder()
                .userId(userId)
                .userThemeId(userTheme.getId())
                .property(property)
                .oldValue(oldValue)
                .newValue(newValue)
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
