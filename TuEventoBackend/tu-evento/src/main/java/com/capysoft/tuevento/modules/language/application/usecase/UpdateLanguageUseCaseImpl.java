package com.capysoft.tuevento.modules.language.application.usecase;

import com.capysoft.tuevento.modules.language.application.dto.request.UpdateLanguageRequest;
import com.capysoft.tuevento.modules.language.application.dto.response.LanguageResponse;
import com.capysoft.tuevento.modules.language.application.mapper.LanguageAppMapper;
import com.capysoft.tuevento.modules.language.application.port.in.UpdateLanguageUseCase;
import com.capysoft.tuevento.modules.language.domain.event.DefaultLanguageChangedEvent;
import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Servicio para actualizar idiomas.
 */
@Service
@RequiredArgsConstructor
public class UpdateLanguageUseCaseImpl implements UpdateLanguageUseCase {

    private final LanguageRepository languageRepository;
    private final LanguageAppMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public LanguageResponse updateLanguage(Long languageId, UpdateLanguageRequest request) {
        Language language = languageRepository.findById(languageId)
                .orElseThrow(() -> new NotFoundException("LANGUAGE_NOT_FOUND", "Language not found with id: " + languageId));

        Language updatedLanguage = Language.builder()
                .languageId(language.getLanguageId())
                .code(language.getCode())
                .name(request.getName().trim())
                .isActive(language.getIsActive())
                .isDefault(language.getIsDefault())
                .build();

        Language savedLanguage = languageRepository.save(updatedLanguage);
        return mapper.toResponse(savedLanguage);
    }

    @Override
    @Transactional
    public LanguageResponse activateLanguage(Long languageId) {
        Language language = languageRepository.findById(languageId)
                .orElseThrow(() -> new NotFoundException("LANGUAGE_NOT_FOUND", "Language not found with id: " + languageId));

        if (Boolean.TRUE.equals(language.getIsActive())) {
            throw new BusinessException("LANGUAGE_ALREADY_ACTIVE", "Language is already active");
        }

        language.activate();
        Language savedLanguage = languageRepository.save(language);
        
        return mapper.toResponse(savedLanguage);
    }

    @Override
    @Transactional
    public LanguageResponse deactivateLanguage(Long languageId) {
        Language language = languageRepository.findById(languageId)
                .orElseThrow(() -> new NotFoundException("LANGUAGE_NOT_FOUND", "Language not found with id: " + languageId));

        if (Boolean.FALSE.equals(language.getIsActive())) {
            throw new BusinessException("LANGUAGE_ALREADY_INACTIVE", "Language is already inactive");
        }

        language.deactivate(); // Lanza excepción si es idioma por defecto
        Language savedLanguage = languageRepository.save(language);
        
        return mapper.toResponse(savedLanguage);
    }

    @Override
    @Transactional
    public LanguageResponse setDefaultLanguage(Long languageId) {
        // Verificar que el idioma existe
        Language language = languageRepository.findById(languageId)
                .orElseThrow(() -> new NotFoundException("LANGUAGE_NOT_FOUND", "Language not found with id: " + languageId));

        // Verificar que el idioma está activo
        if (Boolean.FALSE.equals(language.getIsActive())) {
            throw new BusinessException("LANGUAGE_NOT_ACTIVE", "Cannot set inactive language as default");
        }

        // Verificar que no es ya el idioma por defecto
        if (Boolean.TRUE.equals(language.getIsDefault())) {
            throw new BusinessException("LANGUAGE_ALREADY_DEFAULT", "Language is already the default language");
        }

        // Obtener el idioma por defecto actual
        Optional<Language> currentDefault = languageRepository.findDefaultLanguage();
        Long previousDefaultId = currentDefault.map(Language::getLanguageId).orElse(null);

        // Desmarcar el idioma por defecto actual si existe
        if (currentDefault.isPresent()) {
            Language current = currentDefault.get();
            current.unsetAsDefault();
            languageRepository.save(current);
            // Flush para asegurar que se ejecute antes del siguiente save
            languageRepository.flush();
        }

        // Marcar el nuevo idioma como por defecto
        language.setAsDefault();
        Language savedLanguage = languageRepository.save(language);

        // Publicar evento de dominio
        eventPublisher.publishEvent(new DefaultLanguageChangedEvent(previousDefaultId, languageId));

        return mapper.toResponse(savedLanguage);
    }
}