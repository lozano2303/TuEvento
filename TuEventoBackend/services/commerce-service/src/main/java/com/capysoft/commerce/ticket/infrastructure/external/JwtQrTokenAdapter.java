package com.capysoft.commerce.ticket.infrastructure.external;

import com.capysoft.commerce.ticket.domain.port.QrTokenPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

/**
 * Adaptador JWT (jjwt) para los tokens QR de tickets.
 *
 * Usa un secreto propio (app.qr.secret) distinto al de los access tokens, de
 * modo que un QR nunca pueda confundirse con un token de sesión ni viceversa.
 * La expiración (app.qr.expiration, por defecto 60s) la impone el claim exp,
 * así que un QR caduca criptográficamente aunque el store de nonces se pierda.
 */
@Component
public class JwtQrTokenAdapter implements QrTokenPort {

    private static final String CLAIM_SCOPE  = "scope";
    private static final String CLAIM_TARGET = "target";
    private static final String CLAIM_EVENT  = "event";
    private static final String CLAIM_USER   = "user";
    private static final String CLAIM_NONCE  = "nonce";

    // Si no se define app.qr.secret, cae al secreto JWT (siempre presente).
    @Value("${app.qr.secret:${app.jwt.secret}}")
    private String secret;

    @Value("${app.qr.expiration:60000}")
    private long expirationMillis;

    private SecretKey signingKey;

    @PostConstruct
    void init() {
        signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generate(QrScope scope, Long targetId, Long eventId, Long userId, String nonce) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claim(CLAIM_SCOPE, scope.name())
                .claim(CLAIM_TARGET, targetId)
                .claim(CLAIM_EVENT, eventId)
                .claim(CLAIM_USER, userId)
                .claim(CLAIM_NONCE, nonce)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMillis))
                .signWith(signingKey)
                .compact();
    }

    @Override
    public Optional<QrPayload> parse(String token) {
        try {
            Claims c = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            QrScope scope = QrScope.valueOf(c.get(CLAIM_SCOPE, String.class));
            Long targetId = c.get(CLAIM_TARGET, Long.class);
            Long eventId  = c.get(CLAIM_EVENT, Long.class);
            Long userId   = c.get(CLAIM_USER, Long.class);
            String nonce  = c.get(CLAIM_NONCE, String.class);

            if (targetId == null || eventId == null || userId == null || nonce == null) {
                return Optional.empty();
            }
            return Optional.of(new QrPayload(scope, targetId, eventId, userId, nonce));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public long getExpirationSeconds() {
        return expirationMillis / 1000;
    }
}
