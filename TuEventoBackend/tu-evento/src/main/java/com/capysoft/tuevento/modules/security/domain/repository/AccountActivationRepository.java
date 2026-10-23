package com.capysoft.tuevento.modules.security.domain.repository;

import com.capysoft.tuevento.modules.security.domain.model.AccountActivation;
import com.capysoft.tuevento.modules.security.domain.model.User;

import java.util.Optional;

public interface AccountActivationRepository {

    AccountActivation save(AccountActivation accountActivation);
    
    Optional<AccountActivation> findByUserId(Integer userId);
    
    Optional<AccountActivation> findByActivationCode(String activationCode);
    
    void deleteByUserId(Integer userId);
    
    Optional<AccountActivation> findByUserAndActivationCodeAndActivatedFalse(User user, String activationCode);

    Optional<AccountActivation> findByUserAndActivatedFalse(User user);
}