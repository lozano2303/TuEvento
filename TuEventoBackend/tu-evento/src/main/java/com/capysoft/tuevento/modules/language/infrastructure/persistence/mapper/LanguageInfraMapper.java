package com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper;

import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.LanguageEntity;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Mapper entre dominio e infraestructura para Language.
 */
@Mapper(componentModel = "spring")
public interface LanguageInfraMapper {
    
    Language toDomain(LanguageEntity entity);
    
    LanguageEntity toEntity(Language domain);
    
    List<Language> toDomainList(List<LanguageEntity> entities);
    
    List<LanguageEntity> toEntityList(List<Language> domains);
}