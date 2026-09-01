package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.security.application.dto.OauthProfile;
import com.capysoft.tuevento.modules.security.application.dto.request.GoogleAuthRequest;
import com.capysoft.tuevento.modules.security.application.dto.response.LoginResponse;
import com.capysoft.tuevento.modules.security.application.port.in.GoogleAuthPort;
import com.capysoft.tuevento.modules.security.application.port.in.OauthLoginPort;
import com.capysoft.tuevento.modules.security.application.port.out.GoogleIdTokenVerifierPort;
import com.capysoft.tuevento.modules.security.application.port.out.GoogleIdTokenVerifierPort.GoogleTokenClaims;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Handles the Google Identity Services (GSI) login flow:
 *
 *   Browser → Google popup → credential (id_token JWT)
 *   Browser → POST /api/v1/auth/google { idToken }
 *   Here   → verifies id_token server-side → builds OauthProfile → delegates to OauthLoginUseCase
 *
 * All user-resolution logic (auto-register / lookup / JWT generation) lives in
 * OauthLoginUseCase.loginWithProfile() — not duplicated here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleIdTokenAuthUseCase implements GoogleAuthPort {

    private static final String PROVIDER = "google";

    private final GoogleIdTokenVerifierPort googleVerifier;
    private final OauthLoginPort            oauthLoginPort;

    @Override
    public LoginResponse authenticate(GoogleAuthRequest request) {

        // 1. Cryptographically verify the id_token: signature (JWKS), audience, issuer, expiry.
        GoogleTokenClaims claims = googleVerifier.verify(request.getIdToken());

        // 2. Reject unverified emails — rare but possible with some Google account types.
        if (!claims.emailVerified()) {
            throw new BusinessException("GOOGLE_EMAIL_NOT_VERIFIED",
                    "Tu cuenta de Google no tiene el correo verificado. "
                  + "Verifica tu dirección de correo en Google e intenta de nuevo.");
        }

        log.debug("Google ID Token verified — sub={} email={}", claims.sub(), claims.email());

        // 3. Build an OauthProfile from the verified claims and delegate.
        //    providerUserId = Google subject (sub) — stable, unique per user per app.
        //    alias          = Google display name (used for auto-generated alias on first login).
        OauthProfile profile = OauthProfile.builder()
                .providerUserId(claims.sub())
                .email(claims.email())
                .alias(claims.name())
                .build();

        return oauthLoginPort.loginWithProfile(PROVIDER, profile);
    }
}
