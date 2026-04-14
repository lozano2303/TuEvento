package com.capysoft.tuevento.modules.security.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.security.domain.model.AccountLockout;
import com.capysoft.tuevento.modules.security.domain.model.User;
import com.capysoft.tuevento.modules.security.domain.model.UserStatus;
import com.capysoft.tuevento.modules.security.domain.model.UserStatusHistory;
import com.capysoft.tuevento.modules.security.domain.repository.AccountLockoutRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserStatusHistoryRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserStatusRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountLockoutService {

    private static final String BLOCKED_CODE = "BLOCKED";
    private static final String ACTIVE_CODE  = "ACTIVE";

    private final AccountLockoutRepository    accountLockoutRepository;
    private final UserRepository              userRepository;
    private final UserStatusRepository        userStatusRepository;
    private final UserStatusHistoryRepository userStatusHistoryRepository;

    /**
     * Persists the lockout record in its own transaction so it survives
     * the rollback of the parent login() transaction.
     * When failedAttempts reaches the max, also blocks the user and logs the status change.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLockout(AccountLockout lockout) {
        accountLockoutRepository.save(lockout);

        if (lockout.getLockedUntil() != null) {
            blockUser(lockout.getUser(), "Account temporarily blocked due to too many failed login attempts");
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

    private void blockUser(User user, String reason) {
        UserStatus blocked = userStatusRepository.findByCode(BLOCKED_CODE)
                .orElseThrow(() -> new NotFoundException("STATUS_NOT_FOUND", "BLOCKED status not found"));

        user.setUserStatus(blocked);
        userRepository.save(user);

        userStatusHistoryRepository.save(UserStatusHistory.builder()
                .user(user)
                .userStatus(blocked)
                .reason(reason)
                .build());
    }

    private void unblockUser(Integer userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        if (!BLOCKED_CODE.equals(user.getUserStatus().getCode())) return;

        UserStatus active = userStatusRepository.findByCode(ACTIVE_CODE)
                .orElseThrow(() -> new NotFoundException("STATUS_NOT_FOUND", "ACTIVE status not found"));

        user.setUserStatus(active);
        userRepository.save(user);

        userStatusHistoryRepository.save(UserStatusHistory.builder()
                .user(user)
                .userStatus(active)
                .reason("Account automatically unblocked after lockout period expired")
                .build());
    }
}
