package com.capysoft.tuevento.modules.theme.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.theme.domain.model.UserTheme;
import com.capysoft.tuevento.modules.theme.domain.repository.UserThemeRepository;
import com.capysoft.tuevento.modules.theme.infrastructure.persistence.mapper.ThemeInfraMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserThemeRepositoryImpl implements UserThemeRepository {

    private final UserThemeJpaRepository jpaRepository;
    private final ThemeInfraMapper mapper;

    @Override
    public List<UserTheme> findByUserId(Integer userId) {
        return jpaRepository.findByUserId(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<UserTheme> findActiveByUserId(Integer userId) {
        return jpaRepository.findByUserIdAndIsActiveTrue(userId).map(mapper::toDomain);
    }

    @Override
    public Optional<UserTheme> findById(Integer id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByUserIdAndThemeId(Integer userId, Integer themeId) {
        return jpaRepository.existsByUserIdAndThemeId(userId, themeId);
    }

    @Override
    public UserTheme save(UserTheme userTheme) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(userTheme)));
    }

    @Override
    @Transactional
    public void deactivateAllByUserId(Integer userId) {
        jpaRepository.updateIsActiveByUserId(userId);
    }
}
