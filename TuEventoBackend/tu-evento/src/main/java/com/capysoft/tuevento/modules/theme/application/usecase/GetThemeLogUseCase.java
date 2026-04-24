package com.capysoft.tuevento.modules.theme.application.usecase;

import com.capysoft.tuevento.modules.theme.application.dto.response.ThemeLogResponse;
import com.capysoft.tuevento.modules.theme.application.mapper.ThemeAppMapper;
import com.capysoft.tuevento.modules.theme.application.port.in.GetThemeLogPort;
import com.capysoft.tuevento.modules.theme.domain.repository.ThemeLogRepository;
import com.capysoft.tuevento.modules.theme.domain.repository.UserThemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetThemeLogUseCase implements GetThemeLogPort {

    private final UserThemeRepository userThemeRepository;
    private final ThemeLogRepository  themeLogRepository;
    private final ThemeAppMapper      mapper;

    @Override
    public List<ThemeLogResponse> getLog(Integer userId) {
        return userThemeRepository.findActiveByUserId(userId)
                .map(userTheme -> themeLogRepository.findByUserThemeId(userTheme.getId())
                        .stream()
                        .map(mapper::toThemeLogResponse)
                        .toList())
                .orElse(Collections.emptyList());
    }
}
