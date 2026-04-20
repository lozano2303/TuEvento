package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.security.application.dto.request.RecoverPasswordRequest;
import com.capysoft.tuevento.modules.security.application.port.in.RecoverPasswordPort;
import com.capysoft.tuevento.modules.security.application.port.out.CodeGeneratorPort;
import com.capysoft.tuevento.modules.security.application.port.out.EmailNotificationPort;
import com.capysoft.tuevento.modules.security.domain.model.LoginCredentials;
import com.capysoft.tuevento.modules.security.domain.model.RecoverPassword;
import com.capysoft.tuevento.modules.security.domain.repository.LoginCredentialsRepository;
import com.capysoft.tuevento.modules.security.domain.repository.RecoverPasswordRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RecoverPasswordUseCase implements RecoverPasswordPort {

    private static final int RECOVERY_EXPIRY_MINUTES = 30;

    private final LoginCredentialsRepository loginCredentialsRepository;
    private final RecoverPasswordRepository  recoverPasswordRepository;
    private final CodeGeneratorPort          codeGenerator;
    private final EmailNotificationPort      emailNotification;

    @Override
    @Transactional
    public void recover(RecoverPasswordRequest request) {
        // Explicit error if email not found
        LoginCredentials credentials = loginCredentialsRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("EMAIL_NOT_FOUND", "This email is not registered in the system"));

        recoverPasswordRepository.invalidateAllByUserId(credentials.getUser().getUserId());

        String code = codeGenerator.generateRecoveryCode();

        recoverPasswordRepository.save(RecoverPassword.builder()
                .user(credentials.getUser())
                .code(code)
                .codeStatus(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(RECOVERY_EXPIRY_MINUTES))
                .build());

        emailNotification.sendPasswordRecoveryEmail(
                request.getEmail(), credentials.getUser().getAlias(), code);
    }
}
