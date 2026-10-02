package com.capysoft.tuevento.modules.language.application.usecase;

import com.capysoft.tuevento.modules.language.application.dto.request.CreateLanguageRequest;
import com.capysoft.tuevento.modules.language.application.dto.response.LanguageResponse;
import com.capysoft.tuevento.modules.language.application.mapper.LanguageAppMapper;
import com.capysoft.tuevento.modules.language.application.port.in.CreateLanguageUseCase;
import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para crear nuevos idiomas.
 */
@Service
@RequiredArgsConstructor
public class CreateLanguageUseCaseImpl implements CreateLanguageUseCase {

    private final LanguageRepository languageRepository;
    private final LanguageAppMapper mapper;

    @Override
    @Transactional
    public LanguageResponse execute(CreateLanguageRequest request) {
        String normalizedCode = request.getCode().trim().toLowerCase();
        
        // Verificar que el código normalizado no existe
        if (languageRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new BusinessException("LANGUAGE_CODE_EXISTS", "Language code already exists: " + normalizedCode);
        }

        Language language = Language.builder()
                .code(normalizedCode)
                .name(request.getName().trim())
                .isActive(true)
                .isDefault(false)
                .build();

        Language savedLanguage = languageRepository.save(language);
        return mapper.toResponse(savedLanguage);
    }
}