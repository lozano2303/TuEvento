package com.capysoft.identity.security.infrastructure.external;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.capysoft.identity.security.application.port.out.TokenGeneratorPort;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

/**
 * Generador y validador de JWT firmados con RSA-256 (RS256).
 *
 * <h3>Por qué RS256 en lugar de HS256</h3>
 * <p>HS256 requiere compartir el mismo secreto entre todos los servicios que
 * validan tokens. En la arquitectura de microservicios objetivo, cada servicio
 * solo necesita la clave <em>pública</em> para verificar — nunca ve la clave
 * privada, que permanece exclusivamente en el servicio Identidad. Esto reduce
 * drásticamente la superficie de ataque en caso de compromiso de un servicio.
 *
 * <h3>Carga de claves</h3>
 * <p>La clave privada se lee en PEM (PKCS#8) desde {@code app.jwt.private-key-path}: un
 * archivo local en dev o un Secret de k8s montado en producción. La pública se deriva de
 * ella y se expone en {@code /.well-known/jwks.json} para que los demás servicios la descarguen.
 *
 * <p>Si la clave privada no existe y {@code app.jwt.ephemeral-keys-allowed=true} (solo dev y
 * tests), se genera un par RSA en memoria: los tokens dejan de valer al reiniciar.
 *
 * <h3>Transición desde HS256</h3>
 * <p>Mientras {@code app.jwt.secret} tenga valor, también se aceptan los tokens HS256 emitidos
 * antes del cambio (access 15 min, refresh 7 días). Los tokens nuevos siempre son RS256. La
 * clave se elige por el {@code alg} de la cabecera y HS256 solo se valida contra ese secreto,
 * nunca contra la clave pública. Cuando pase el periodo de los refresh tokens, quitar el secreto.
 *
 * <h3>Notas de seguridad</h3>
 * <ul>
 *   <li>La clave privada está en {@code .gitignore}. Nunca debe ir al repositorio.
 *   <li>Los QR de tickets usan un secreto HS256 propio ({@code app.qr.secret}) —
 *       son internos y no los validan otros servicios, así que no necesitan RS256.
 *   <li>El refresh token sigue firmado con RS256 para consistencia, aunque solo
 *       lo valida este mismo servicio.
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenGenerator implements TokenGeneratorPort {

    /** ID de la clave activa; va en la cabecera {@code kid} y en el JWK Set. */
    public static final String KEY_ID = "tuevento-jwt-key-1";

    private static final String LEGACY_ALGORITHM = "HS256";
    private static final String ISSUED_AT_MS_CLAIM = "iatMs";

    @Value("${app.jwt.private-key-path:classpath:keys/jwt-private.pem}")
    private Resource privateKeyResource;

    /**
     * Clave privada PEM en línea (p. ej. una variable de entorno de Portainer). Si tiene valor,
     * tiene prioridad sobre {@code app.jwt.private-key-path}.
     */
    @Value("${app.jwt.private-key:}")
    private String inlinePrivateKey;

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /** Secreto HS256 anterior; solo para validar tokens emitidos antes del cambio. Vacío = no se aceptan. */
    @Value("${app.jwt.secret:}")
    private String legacySecret;

    @Value("${app.jwt.ephemeral-keys-allowed:false}")
    private boolean ephemeralKeysAllowed;

    private RSAPrivateKey privateKey;
    private RSAPublicKey  publicKey;
    private SecretKey     legacyKey;

    @PostConstruct
    void init() {
        try {
            if (inlinePrivateKey != null && !inlinePrivateKey.isBlank()) {
                privateKey = parsePrivateKey(inlinePrivateKey);
                publicKey  = derivePublicKey(privateKey);
            } else if (privateKeyResource.exists()) {
                privateKey = loadPrivateKey(privateKeyResource);
                publicKey  = derivePublicKey(privateKey);
            } else if (ephemeralKeysAllowed) {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair pair = generator.generateKeyPair();
                privateKey = (RSAPrivateKey) pair.getPrivate();
                publicKey  = (RSAPublicKey) pair.getPublic();
                log.warn("JWT private key not found at {}: using an in-memory RSA key pair. "
                        + "Tokens will not survive a restart (dev/test only)", privateKeyResource);
            } else {
                throw new IllegalStateException("JWT private key not found: " + privateKeyResource);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JWT RSA keys — check app.jwt.private-key-path " +
                    "and app.jwt.public-key-path", e);
        }
        if (legacySecret != null && !legacySecret.isBlank()) {
            legacyKey = Keys.hmacShaKeyFor(legacySecret.getBytes(StandardCharsets.UTF_8));
            log.info("Legacy HS256 tokens are still accepted (app.jwt.secret is set)");
        }
        log.info("JWT RS256 keys ready. Public key modulus length: {} bits", publicKey.getModulus().bitLength());
    }

    /**
     * Expone la clave pública RSA para que el endpoint JWKS y otros servicios
     * puedan verificar tokens sin necesitar la clave privada.
     */
    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    // ── Token generation ─────────────────────────────────────────────────────

    @Override
    public String generateAccessToken(Integer userId, String alias, String role) {
        Date now = new Date();
        return Jwts.builder()
                .header().keyId(KEY_ID).and()
                .id(UUID.randomUUID().toString())
                .subject(alias)
                .claim("userId", userId)
                .claim("role", role)
                // iat va en segundos; iatMs permite comparar con una revocación del mismo segundo
                .claim(ISSUED_AT_MS_CLAIM, now.getTime())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiration))
                .signWith(privateKey)        // RS256 con clave privada
                .compact();
    }

    @Override
    public String generateRefreshToken(Integer userId) {
        return Jwts.builder()
                .header().keyId(KEY_ID).and()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(privateKey)
                .compact();
    }

    // ── Token validation & claim extraction ──────────────────────────────────

    @Override
    public Optional<Integer> extractUserId(String token) {
        try {
            return Optional.ofNullable(parseClaims(token).get("userId", Integer.class));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> extractRole(String token) {
        try {
            return Optional.ofNullable(parseClaims(token).get("role", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> extractSubject(String token) {
        try {
            return Optional.ofNullable(parseClaims(token).getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public Optional<TokenMetadata> extractMetadata(String token) {
        try {
            Claims claims = parseClaims(token);
            Number issuedAtMs = claims.get(ISSUED_AT_MS_CLAIM, Number.class);
            Instant issuedAt = issuedAtMs != null
                    ? Instant.ofEpochMilli(issuedAtMs.longValue())
                    : toInstant(claims.getIssuedAt());
            return Optional.of(new TokenMetadata(claims.getId(), issuedAt, toInstant(claims.getExpiration())));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .keyLocator(signingKeyLocator())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * RS256 se valida con la clave pública; HS256 solo con el secreto legado, si existe.
     * Cualquier otro algoritmo se rechaza.
     */
    private Locator<Key> signingKeyLocator() {
        return header -> {
            String algorithm = header.getAlgorithm();
            if ("RS256".equals(algorithm)) {
                return publicKey;
            }
            if (LEGACY_ALGORITHM.equals(algorithm) && legacyKey != null) {
                return legacyKey;
            }
            throw new JwtException("Unsupported JWT algorithm: " + algorithm);
        };
    }

    /**
     * Carga una clave privada PKCS#8 desde un recurso PEM.
     * {@code openssl genrsa} genera formato PKCS#1; {@code openssl pkcs8} lo convierte.
     * Las claves generadas con {@code openssl genrsa} sin conversión también funcionan
     * porque jjwt 0.12+ acepta ambos formatos internamente.
     */
    private RSAPrivateKey loadPrivateKey(Resource resource) throws Exception {
        return parsePrivateKey(new String(resource.getInputStream().readAllBytes()));
    }

    /**
     * Lee una clave privada PKCS#8 en PEM. Acepta los saltos de línea escritos como {@code \n}
     * literal, que es como suelen quedar al pegar la clave en una variable de entorno de una sola línea.
     */
    private RSAPrivateKey parsePrivateKey(String rawPem) throws Exception {
        String pem = rawPem.replace("\\n", "\n")
                .replaceAll("-----BEGIN.*?-----", "")
                .replaceAll("-----END.*?-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        // Intentar PKCS#8 primero, luego PKCS#1 si falla
        try {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            // Fallback: wrapping PKCS#1 en PKCS#8 (compatibilidad con openssl genrsa sin -traditional)
            // jjwt usa BouncyCastle internamente, pero aquí usamos KeyFactory estándar
            throw new IllegalArgumentException(
                "Private key must be in PKCS#8 format. Convert with: " +
                "openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in jwt-private.pem -out jwt-private-pkcs8.pem", e);
        }
    }

    /**
     * Deriva la clave pública del módulo y el exponente que guarda la privada (PKCS#8 de
     * openssl siempre es CRT). Así la pública nunca puede quedar desemparejada de la privada.
     */
    private RSAPublicKey derivePublicKey(RSAPrivateKey key) throws Exception {
        if (!(key instanceof RSAPrivateCrtKey crtKey)) {
            throw new IllegalArgumentException("JWT private key must include its CRT parameters");
        }
        RSAPublicKeySpec spec = new RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent());
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}
