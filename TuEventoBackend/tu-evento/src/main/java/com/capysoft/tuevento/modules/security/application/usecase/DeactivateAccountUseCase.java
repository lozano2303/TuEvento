package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.security.application.dto.request.DeactivateAccountRequest;
import com.capysoft.tuevento.modules.security.application.port.in.DeactivateAccountPort;
import com.capysoft.tuevento.modules.security.application.port.out.PasswordEncoderPort;
import com.capysoft.tuevento.modules.security.domain.model.AuthSession;
import com.capysoft.tuevento.modules.security.domain.model.LoginCredentials;
import com.capysoft.tuevento.modules.security.domain.model.RefreshToken;
import com.capysoft.tuevento.modules.security.domain.model.User;
import com.capysoft.tuevento.modules.security.domain.model.UserStatus;
import com.capysoft.tuevento.modules.security.domain.model.UserStatusHistory;
import com.capysoft.tuevento.modules.security.domain.repository.AuthSessionRepository;
import com.capysoft.tuevento.modules.security.domain.repository.LoginCredentialsRepository;
import com.capysoft.tuevento.modules.security.domain.repository.RefreshTokenRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserStatusHistoryRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserStatusRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeactivateAccountUseCase implements DeactivateAccountPort {

    private static final String INACTIVE_STATUS_CODE = "INACTIVE";
    private static final String DEACTIVATION_REASON  = "User requested account deactivation";

    private final LoginCredentialsRepository loginCredentialsRepository;
    private final UserRepository             userRepository;
    private final UserStatusRepository       userStatusRepository;
    private final UserStatusHistoryRepository userStatusHistoryRepository;
    private final AuthSessionRepository      authSessionRepository;
    private final RefreshTokenRepository     refreshTokenRepository;
    private final PasswordEncoderPort        passwordEncoder;

    @Override
    @Transactional
    public void deactivate(DeactivateAccountRequest request) {
        // 1. Resolve the authenticated user from the JWT principal
        SecurityUser securityUser = (SecurityUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        // 2. Load credentials to verify the password
        LoginCredentials credentials = loginCredentialsRepository
                .findByUserId(securityUser.getUserId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        if (!passwordEncoder.matches(request.getPassword(), credentials.getPasswordHash())) {
            throw new BusinessException("INVALID_PASSWORD", "Password is incorrect");
        }

        User user = credentials.getUser();

        // 3. Guard: cannot deactivate an already inactive or deleted account
        String currentStatus = user.getUserStatus().getCode();
        if (INACTIVE_STATUS_CODE.equals(currentStatus)) {
            throw new BusinessException("ACCOUNT_ALREADY_INACTIVE", "Account is already inactive");
        }
        if ("DELETED".equals(currentStatus)) {
            throw new BusinessException("ACCOUNT_DELETED", "Account not found");
        }

        // 4. Resolve the INACTIVE UserStatus entity
        UserStatus inactiveStatus = userStatusRepository.findByCode(INACTIVE_STATUS_CODE)
                .orElseThrow(() -> new NotFoundException("STATUS_NOT_FOUND", "INACTIVE status not found"));

        // 5. Set user status to INACTIVE
        user.setUserStatus(inactiveStatus);
        userRepository.save(user);

        // 6. Audit trail — record the status transition
        userStatusHistoryRepository.save(UserStatusHistory.builder()
                .user(user)
                .userStatus(inactiveStatus)
                .reason(DEACTIVATION_REASON)
                .build());

        // 7. Revoke all active sessions and refresh tokens for this user
        //    so existing JWTs cannot be used to keep the session alive after deactivation.
        List<AuthSession> activeSessions = authSessionRepository.findActiveByUserId(user.getUserId());
        LocalDateTime now = LocalDateTime.now();
        for (AuthSession session : activeSessions) {
            session.setRevoked(true);
            session.setRevokedAt(now);
            authSessionRepository.save(session);

            refreshTokenRepository.findByAuthSessionId(session.getAuthSessionId())
                    .ifPresent(rt -> {
                        rt.setRevoked(true);
                        rt.setRevokedAt(now);
                        refreshTokenRepository.save(rt);
                    });
        }
    }
}
