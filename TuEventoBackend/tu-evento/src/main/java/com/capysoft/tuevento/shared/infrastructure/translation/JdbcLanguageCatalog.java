package com.capysoft.tuevento.shared.infrastructure.translation;

import com.capysoft.tuevento.shared.application.translation.LanguageCatalog;
import com.capysoft.tuevento.shared.application.translation.LanguageSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * {@link LanguageCatalog} leído de la tabla language. El catálogo lo administra el servicio de
 * localización; el monolito solo lo lee, en la misma base de datos, en cualquier modo de traducción.
 */
@Component
@RequiredArgsConstructor
public class JdbcLanguageCatalog implements LanguageCatalog {

    private static final RowMapper<LanguageSummary> SUMMARY = (rs, row) -> LanguageSummary.builder()
            .languageId(rs.getInt("language_id"))
            .code(rs.getString("code"))
            .build();

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<LanguageSummary> findByCode(String code) {
        return jdbcTemplate.query("SELECT language_id, code FROM language WHERE code = ?", SUMMARY, code)
                .stream().findFirst();
    }

    @Override
    public Optional<LanguageSummary> findById(Integer languageId) {
        return jdbcTemplate.query("SELECT language_id, code FROM language WHERE language_id = ?", SUMMARY, languageId)
                .stream().findFirst();
    }
}
