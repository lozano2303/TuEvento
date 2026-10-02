package com.capysoft.tuevento.modules.language.application.usecase;

import com.capysoft.tuevento.modules.language.application.dto.response.LanguageResponse;
import com.capysoft.tuevento.modules.language.application.mapper.LanguageAppMapper;
import com.capysoft.tuevento.modules.language.application.port.in.GetLanguagesUseCase;
import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para consultar idiomas.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetLanguagesUseCaseImpl implements GetLanguagesUseCase {

    private final LanguageRepository languageRepository;
    private final LanguageAppMapper mapper;

    @Override
    public List<LanguageResponse> getAllActiveLanguages() {
        List<Language> languages = languageRepository.findAllActive();
        return mapper.toResponseList(languages);
    }

    @Override
    public List<LanguageResponse> getAllLanguages() {
        List<Language> languages = languageRepository.findAll();
        return mapper.toResponseList(languages);
    }

    @Override
    public Optional<LanguageResponse> getLanguageById(Long languageId) {
        return languageRepository.findById(languageId)
                .map(mapper::toResponse);
    }
}