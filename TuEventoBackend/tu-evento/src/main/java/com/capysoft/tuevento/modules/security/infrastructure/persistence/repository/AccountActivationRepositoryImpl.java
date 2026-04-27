package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.domain.model.AccountActivation;
import com.capysoft.tuevento.modules.security.domain.model.User;
import com.capysoft.tuevento.modules.security.domain.repository.AccountActivationRepository;
import com.capysoft.tuevento.modules.security.application.mapper.AccountActivationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountActivationRepositoryImpl implements AccountActivationRepository {

    private final SpringDataAccountActivationRepository springDataRepository;
    private final AccountActivationMapper mapper;

    @Override
    public AccountActivation save(AccountActivation accountActivation) {
        var entity = mapper.toEntity(accountActivation);
        var savedEntity = springDataRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<AccountActivation> findByUserId(Integer userId) {
        return springDataRepository.findByUser_UserId(userId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<AccountActivation> findByActivationCode(String activationCode) {
        return springDataRepository.findByActivationCode(activationCode)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteByUserId(Integer userId) {
        springDataRepository.deleteByUser_UserId(userId);
    }

    // ✅ NUEVO MÉTODO
    @Override
    public Optional<AccountActivation> findByUserAndActivationCodeAndActivatedFalse(User user, String activationCode) {
        return springDataRepository.findByUser_UserIdAndActivationCodeAndActivatedFalse(user.getUserId(), activationCode)
                .map(mapper::toDomain);
    }

    // ✅ NUEVO MÉTODO
    @Override
    public Optional<AccountActivation> findByUserAndActivatedFalse(User user) {
        return springDataRepository.findTopByUser_UserIdAndActivatedFalseOrderByCreatedAtDesc(user.getUserId())
                .map(mapper::toDomain);
    }
}