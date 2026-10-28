package com.capysoft.identity.theme.infrastructure.persistence.mapper;

import com.capysoft.identity.theme.domain.model.Theme;
import com.capysoft.identity.theme.domain.model.ThemeCustomization;
import com.capysoft.identity.theme.domain.model.ThemeLog;
import com.capysoft.identity.theme.domain.model.UserTheme;
import com.capysoft.identity.theme.infrastructure.persistence.entity.ThemeCustomizationEntity;
import com.capysoft.identity.theme.infrastructure.persistence.entity.ThemeEntity;
import com.capysoft.identity.theme.infrastructure.persistence.entity.ThemeLogEntity;
import com.capysoft.identity.theme.infrastructure.persistence.entity.UserThemeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ThemeInfraMapper {

    // ── Theme ──────────────────────────────────────────────────────────────

    @Mapping(source = "id", target = "themeId")
    ThemeEntity toEntity(Theme domain);

    @Mapping(source = "themeId", target = "id")
    Theme toDomain(ThemeEntity entity);

    // ── UserTheme ──────────────────────────────────────────────────────────

    @Mapping(source = "id", target = "userThemeId")
    UserThemeEntity toEntity(UserTheme domain);

    @Mapping(source = "userThemeId", target = "id")
    UserTheme toDomain(UserThemeEntity entity);

    // ── ThemeCustomization ─────────────────────────────────────────────────

    @Mapping(source = "id", target = "customizationId")
    ThemeCustomizationEntity toEntity(ThemeCustomization domain);

    @Mapping(source = "customizationId", target = "id")
    ThemeCustomization toDomain(ThemeCustomizationEntity entity);

    // ── ThemeLog ───────────────────────────────────────────────────────────

    @Mapping(source = "id", target = "logId")
    ThemeLogEntity toEntity(ThemeLog domain);

    @Mapping(source = "logId", target = "id")
    ThemeLog toDomain(ThemeLogEntity entity);
}
