package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.security.application.dto.request.ReactivateAccountRequest;
import com.capysoft.tuevento.modules.security.application.port.in.RequestReactivationPort;
import com.capysoft.tuevento.modules.security.application.port.out.CodeGeneratorPort;
import com.capysoft.tuevento.modules.security.application.port.out.EmailNotificationPort;
import com.capysoft.tuevento.modules.security.domain.model.AccountReactivationToken;
import com.capysoft.tuevento.modules.security.domain.model.LoginCredentials;
import com.capysoft.tuevento.modules.security.domain.repository.AccountReactivationTokenRepository;
import com.capysoft.tuevento.modules.security.domain.repository.LoginCredentialsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Generates a reactivation token for a deactivated account and sends it by email.
 *
 * Security note: if the email is not found or belongs to a non-INACTIVE account,
 * the response is identical to a successful request to avoid user enumeration.
 * The actual token is only sent when the account is genuinely INACTIVE.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequestReactivationUseCase implements RequestReactivationPort {

    private static final int    TOKEN_EXPIRY_MINUTES  = 30;
    private static final String INACTIVE_STATUS_CODE  = "INACTIVE";

    private final LoginCredentialsRepository       loginCredentialsRepository;
    private final AccountReactivationTokenRepository reactivationTokenRepository;
    private final CodeGeneratorPort                codeGenerator;
    private final EmailNotificationPort            emailNotification;

    @Override
    @Transactional
    public void request(ReactivateAccountRequest request) {
        // Fail-silent: resolve credentials quietly — no exception propagated to caller
        var credentialsOpt = loginCredentialsRepository.findByEmail(request.getEmail());
        if (credentialsOpt.isEmpty()) {
            log.debug("Reactivation requested for unknown email — silently ignored");
            return;
        }

        LoginCredentials credentials = credentialsOpt.get();
        String statusCode = credentials.getUser().getUserStatus().getCode();

        // Only INACTIVE accounts are eligible for reactivation
        if (!INACTIVE_STATUS_CODE.equals(statusCode)) {
            log.debug("Reactivation requested for account with status={} — silently ignored", statusCode);
            return;
        }

        // Invalidate any previous pending tokens for this user before issuing a new one
        reactivationTokenRepository.invalidateAllByUserId(credentials.getUser().getUserId());

        String token = codeGenerator.generateRecoveryCode();

        reactivationTokenRepository.save(AccountReactivationToken.builder()
                .user(credentials.getUser())
                .token(token)
                .tokenStatus(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES))
                .build());

        try {
            emailNotification.sendReactivationEmail(
                    request.getEmail(),
                    credentials.getUser().getAlias(),
                    token);
        } catch (Exception e) {
            // Fail-open on email: token is already saved; user can request again.
            // This mirrors the behaviour of RegisterUserUseCase#resendActivationCode.
            log.error("Failed to send reactivation email to {} — token saved, email not sent: {}",
                    request.getEmail(), e.getMessage());
        }
    }
}
