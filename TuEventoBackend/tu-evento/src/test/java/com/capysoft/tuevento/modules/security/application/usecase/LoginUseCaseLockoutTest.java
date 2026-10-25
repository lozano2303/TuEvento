package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.security.application.dto.request.LoginRequest;
import com.capysoft.tuevento.modules.security.application.dto.response.LoginResponse;
import com.capysoft.tuevento.modules.security.domain.model.Role;
import com.capysoft.tuevento.modules.security.application.port.out.PasswordEncoderPort;
import com.capysoft.tuevento.modules.security.application.port.out.TokenGeneratorPort;
import com.capysoft.tuevento.modules.security.domain.event.UserLockedEvent;
import com.capysoft.tuevento.modules.security.domain.model.AccountLockout;
import com.capysoft.tuevento.modules.security.domain.model.LoginCredentials;
import com.capysoft.tuevento.modules.security.domain.model.User;
import com.capysoft.tuevento.modules.security.domain.model.UserStatus;
import com.capysoft.tuevento.modules.security.domain.repository.AccountLockoutRepository;
import com.capysoft.tuevento.modules.security.domain.repository.AuthSessionRepository;
import com.capysoft.tuevento.modules.security.domain.repository.LoginCredentialsRepository;
import com.capysoft.tuevento.modules.security.domain.repository.RefreshTokenRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del bloqueo de cuenta por intentos fallidos: la duración sale de
 * {@code security.lockout.minutes} en lugar de un valor fijo.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoginUseCase — bloqueo de cuenta configurable")
class LoginUseCaseLockoutTest {

    @Mock LoginCredentialsRepository loginCredentialsRepository;
    @Mock AccountLockoutRepository   accountLockoutRepository;
    @Mock AccountLockoutService      accountLockoutService;
    @Mock AuthSessionRepository      authSessionRepository;
    @Mock RefreshTokenRepository     refreshTokenRepository;
    @Mock PasswordEncoderPort        passwordEncoder;
    @Mock TokenGeneratorPort         tokenGenerator;
    @Mock ApplicationEventPublisher  eventPublisher;

    @InjectMocks
    LoginUseCase useCase;

    private static final String  EMAIL   = "user@test.com";
    private static final Integer USER_ID = 9;

    private User user;
    private LoginRequest request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "lockoutMinutes", 30L);

        user = User.builder()
                .userId(USER_ID)
                .alias("user")
                .activated(true)
                .userStatus(UserStatus.builder().code("ACTIVE").build())
                .build();
        LoginCredentials credentials = LoginCredentials.builder()
                .user(user).email(EMAIL).passwordHash("hash").build();
        when(loginCredentialsRepository.findByEmail(EMAIL)).thenReturn(Optional.of(credentials));

        request = LoginRequest.builder().email(EMAIL).password("wrong-password").build();
    }

    @Test
    @DisplayName("el quinto intento fallido bloquea la cuenta durante los minutos configurados")
    void fifthFailedAttemptLocksForConfiguredMinutes() {
        AccountLockout lockout = AccountLockout.builder()
                .user(user).failedAttempts(4).windowStartedAt(LocalDateTime.now()).build();
        when(accountLockoutRepository.findByUserId(USER_ID)).thenReturn(Optional.of(lockout));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> useCase.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("demasiados intentos");

        assertThat(lockout.getLockedUntil())
                .isCloseTo(LocalDateTime.now().plusMinutes(30), within(5, SECONDS));
        ArgumentCaptor<UserLockedEvent> event = ArgumentCaptor.forClass(UserLockedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().getLockedUntil()).isEqualTo(lockout.getLockedUntil());
        verify(accountLockoutService).saveLockout(lockout);
    }

    @Test
    @DisplayName("tras vencer el bloqueo el primer login entra aunque el usuario en caché siga BLOCKED")
    void expiredLockoutLetsFirstLoginIn() {
        user.setUserStatus(UserStatus.builder().code("BLOCKED").build());
        user.setRole(Role.builder().code("USER").build());
        AccountLockout lockout = AccountLockout.builder()
                .user(user).failedAttempts(5).lockedUntil(LocalDateTime.now().minusSeconds(5)).build();
        when(accountLockoutRepository.findByUserId(USER_ID)).thenReturn(Optional.of(lockout));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(tokenGenerator.generateAccessToken(USER_ID, "user", "USER")).thenReturn("access");
        when(tokenGenerator.generateRefreshToken(USER_ID)).thenReturn("refresh");
        when(authSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoginResponse response = useCase.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access");
        verify(accountLockoutService, atLeastOnce()).deleteLockout(USER_ID);
    }

    @Test
    @DisplayName("un usuario BLOCKED sin bloqueo vencido sigue rechazado")
    void blockedUserWithoutExpiredLockoutIsRejected() {
        user.setUserStatus(UserStatus.builder().code("BLOCKED").build());
        when(accountLockoutRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("contacta a soporte");
    }

    @Test
    @DisplayName("un bloqueo vigente rechaza el login sin comprobar la contraseña")
    void activeLockoutRejectsLogin() {
        AccountLockout lockout = AccountLockout.builder()
                .user(user).failedAttempts(5).lockedUntil(LocalDateTime.now().plusMinutes(10)).build();
        when(accountLockoutRepository.findByUserId(USER_ID)).thenReturn(Optional.of(lockout));

        assertThatThrownBy(() -> useCase.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("temporalmente");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
}
