package com.capysoft.tuevento.modules.security.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.security.domain.model.AccountLockout;
import com.capysoft.tuevento.modules.security.domain.repository.AccountLockoutRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountLockoutService {

    private final AccountLockoutRepository accountLockoutRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLockout(AccountLockout lockout) {
        accountLockoutRepository.save(lockout);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteLockout(Integer userId) {
        accountLockoutRepository.deleteByUserId(userId);
    }
}
