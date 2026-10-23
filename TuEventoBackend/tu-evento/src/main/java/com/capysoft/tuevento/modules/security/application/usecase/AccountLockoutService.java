package com.capysoft.tuevento.modules.security.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.security.domain.model.AccountLockout;
import com.capysoft.tuevento.modules.security.domain.repository.AccountLockoutRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.UserStatusHistoryEntity;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.UserJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.UserStatusHistoryJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.UserStatusJpaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountLockoutService {

    private static final String BLOCKED_CODE = "BLOCKED";
    private static final String ACTIVE_CODE  = "ACTIVE";

    private final AccountLockoutRepository       accountLockoutRepository;
    private final UserJpaRepository              userJpaRepository;
    private final UserStatusJpaRepository        userStatusJpaRepository;
    private final UserStatusHistoryJpaRepository userStatusHistoryJpaRepository;

    /**
     * Persists the lockout record in its own transaction so it survives
     * the rollback of the parent login() transaction.
     * When failedAttempts reaches the max, also blocks the user and logs the status change.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLockout(AccountLockout lockout) {
        accountLockoutRepository.save(lockout);

        if (lockout.getLockedUntil() != null) {
            blockUser(lockout.getUser().getUserId(),
                    "Account temporarily blocked due to too many failed login attempts");
        }
    }

    /**
     * Deletes the lockout record and restores the user status to ACTIVE.
     * Called on successful login or when the lockout window has expired.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteLockout(Integer userId) {
        accountLockoutRepository.deleteByUserId(userId);
        unblockUser(userId);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private void blockUser(Integer userId, String reason) {
        var blockedStatus = userStatusJpaRepository.findByCode(BLOCKED_CODE).orElse(null);
        if (blockedStatus == null) return;

        userJpaRepository.updateStatusByUserId(userId, BLOCKED_CODE);

        var userRef = userJpaRepository.getReferenceById(userId);
        userStatusHistoryJpaRepository.save(UserStatusHistoryEntity.builder()
                .user(userRef)
                .userStatus(blockedStatus)
                .reason(reason)
                .build());
    }

    private void unblockUser(Integer userId) {
        String currentCode = userJpaRepository.findStatusCodeByUserId(userId);
        if (currentCode == null || !BLOCKED_CODE.equals(currentCode)) return;

        var activeStatus = userStatusJpaRepository.findByCode(ACTIVE_CODE).orElse(null);
        if (activeStatus == null) return;

        userJpaRepository.updateStatusByUserId(userId, ACTIVE_CODE);

        var userRef = userJpaRepository.getReferenceById(userId);
        userStatusHistoryJpaRepository.save(UserStatusHistoryEntity.builder()
                .user(userRef)
                .userStatus(activeStatus)
                .reason("Account automatically unblocked after lockout period expired")
                .build());
    }
}
