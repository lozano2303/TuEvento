package com.capysoft.tuevento.modules.theme.application.usecase;

import com.capysoft.tuevento.modules.theme.application.dto.response.ThemeResponse;
import com.capysoft.tuevento.modules.theme.application.mapper.ThemeAppMapper;
import com.capysoft.tuevento.modules.theme.application.port.in.GetThemesPort;
import com.capysoft.tuevento.modules.theme.domain.repository.ThemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetThemesUseCase implements GetThemesPort {

    private final ThemeRepository themeRepository;
    private final ThemeAppMapper mapper;

    @Override
    public List<ThemeResponse> getAll() {
        return themeRepository.findAll().stream()
                .map(mapper::toThemeResponse)
                .toList();
    }
}
