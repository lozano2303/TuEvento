package com.capysoft.identity.security.domain.repository;

import com.capysoft.identity.security.domain.model.AccountActivation;
import com.capysoft.identity.security.domain.model.User;

import java.util.Optional;

public interface AccountActivationRepository {

    AccountActivation save(AccountActivation accountActivation);
    
    Optional<AccountActivation> findByUserId(Integer userId);
    
    Optional<AccountActivation> findByActivationCode(String activationCode);
    
    void deleteByUserId(Integer userId);
    
    Optional<AccountActivation> findByUserAndActivationCodeAndActivatedFalse(User user, String activationCode);

    Optional<AccountActivation> findByUserAndActivatedFalse(User user);
}