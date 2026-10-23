package com.capysoft.tuevento.modules.theme.application.usecase;

import com.capysoft.tuevento.modules.theme.application.dto.response.ResolvedPaletteResponse;
import com.capysoft.tuevento.modules.theme.application.port.in.ActivateThemePort;
import com.capysoft.tuevento.modules.theme.domain.event.ThemeActivatedEvent;
import com.capysoft.tuevento.modules.theme.domain.model.Theme;
import com.capysoft.tuevento.modules.theme.domain.model.ThemeCustomization;
import com.capysoft.tuevento.modules.theme.domain.model.UserTheme;
import com.capysoft.tuevento.modules.theme.domain.repository.ThemeCustomizationRepository;
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
public class ActivateThemeUseCase implements ActivateThemePort {

    private final ThemeRepository              themeRepository;
    private final UserThemeRepository          userThemeRepository;
    private final ThemeCustomizationRepository customizationRepository;
    private final ThemePaletteResolver         paletteResolver;
    private final ApplicationEventPublisher    eventPublisher;

    @Override
    @Transactional
    public ResolvedPaletteResponse activate(Integer userId, Integer themeId) {
        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new NotFoundException("THEME_NOT_FOUND",
                        "Theme not found with id: " + themeId));

        // Deactivate any currently active user_theme for this user
        userThemeRepository.deactivateAllByUserId(userId);

        // Reactivate existing record or create a new one
        Optional<UserTheme> existing = userThemeRepository.findByUserId(userId).stream()
                .filter(ut -> ut.getThemeId().equals(themeId))
                .findFirst();

        UserTheme userTheme;
        if (existing.isPresent()) {
            UserTheme toReactivate = existing.get();
            toReactivate.setIsActive(true);
            toReactivate.setUpdatedAt(LocalDateTime.now());
            userTheme = userThemeRepository.save(toReactivate);
        } else {
            userTheme = userThemeRepository.save(UserTheme.builder()
                    .userId(userId)
                    .themeId(themeId)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());
        }

        eventPublisher.publishEvent(ThemeActivatedEvent.builder()
                .userId(userId)
                .themeId(themeId)
                .userThemeId(userTheme.getId())
                .build());

        return buildResponse(theme, userTheme);
    }

    private ResolvedPaletteResponse buildResponse(Theme theme, UserTheme userTheme) {
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
