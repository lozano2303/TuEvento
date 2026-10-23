package com.capysoft.tuevento.modules.security.application;

import com.capysoft.tuevento.modules.security.application.dto.request.ActivateAccountRequest;
import com.capysoft.tuevento.modules.security.application.usecase.ActivateAccountUseCase;
import com.capysoft.tuevento.modules.security.domain.event.UserActivatedEvent;
import com.capysoft.tuevento.modules.security.domain.model.AccountActivation;
import com.capysoft.tuevento.modules.security.domain.model.LoginCredentials;
import com.capysoft.tuevento.modules.security.domain.model.User;
import com.capysoft.tuevento.modules.security.domain.model.UserStatus;
import com.capysoft.tuevento.modules.security.domain.repository.AccountActivationRepository;
import com.capysoft.tuevento.modules.security.domain.repository.LoginCredentialsRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserStatusRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Flujo de activación de cuenta: código válido activa la cuenta; código inválido o
 * expirado la deja intacta.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ActivateAccountUseCase")
class ActivateAccountUseCaseTest {

    @Mock private AccountActivationRepository accountActivationRepository;
    @Mock private LoginCredentialsRepository  loginCredentialsRepository;
    @Mock private UserRepository              userRepository;
    @Mock private UserStatusRepository        userStatusRepository;
    @Mock private ApplicationEventPublisher   eventPublisher;

    @InjectMocks private ActivateAccountUseCase useCase;

    private static final String EMAIL = "ana@test.com";
    private static final String CODE  = "123456";

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().userId(7).alias("ana").activated(false).build();
        when(loginCredentialsRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(LoginCredentials.builder().email(EMAIL).user(user).build()));
    }

    @Test
    @DisplayName("Código válido activa la cuenta y publica el evento")
    void valid_code_activates_account() {
        AccountActivation activation = AccountActivation.builder()
                .user(user).activationCode(CODE).activated(false)
                .expiresAt(LocalDateTime.now().plusHours(1)).build();
        when(accountActivationRepository.findByUserAndActivationCodeAndActivatedFalse(user, CODE))
                .thenReturn(Optional.of(activation));
        when(userStatusRepository.findByCode("ACTIVE"))
                .thenReturn(Optional.of(UserStatus.builder().code("ACTIVE").build()));

        useCase.activate(request(CODE));

        assertTrue(activation.getActivated());
        assertTrue(user.getActivated());
        assertEquals("ACTIVE", user.getUserStatus().getCode());
        verify(userRepository).save(user);
        verify(eventPublisher).publishEvent(any(UserActivatedEvent.class));
    }

    @Test
    @DisplayName("Código inválido lanza INVALID_ACTIVATION_CODE sin activar")
    void invalid_code_is_rejected() {
        when(accountActivationRepository.findByUserAndActivationCodeAndActivatedFalse(user, "000000"))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> useCase.activate(request("000000")));

        assertEquals("INVALID_ACTIVATION_CODE", ex.getCode());
        assertFalse(user.getActivated());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Código expirado lanza ACTIVATION_CODE_EXPIRED sin activar")
    void expired_code_is_rejected() {
        AccountActivation expired = AccountActivation.builder()
                .user(user).activationCode(CODE).activated(false)
                .expiresAt(LocalDateTime.now().minusMinutes(1)).build();
        when(accountActivationRepository.findByUserAndActivationCodeAndActivatedFalse(user, CODE))
                .thenReturn(Optional.of(expired));

        BusinessException ex = assertThrows(BusinessException.class, () -> useCase.activate(request(CODE)));

        assertEquals("ACTIVATION_CODE_EXPIRED", ex.getCode());
        assertFalse(user.getActivated());
        verify(userRepository, never()).save(any());
    }

    private ActivateAccountRequest request(String code) {
        return ActivateAccountRequest.builder().email(EMAIL).activationCode(code).build();
    }
}
