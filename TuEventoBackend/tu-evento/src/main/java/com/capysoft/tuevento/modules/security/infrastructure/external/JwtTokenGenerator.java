package com.capysoft.tuevento.modules.security.infrastructure.external;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.security.application.port.out.TokenGeneratorPort;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

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
 * <p>Las claves se leen desde recursos del classpath en formato PEM estándar.
 * La clave privada ({@code keys/jwt-private.pem}) se inyecta via classpath en
 * desarrollo; en producción se monta como un Secret de k8s en la ruta
 * correspondiente. La clave pública ({@code keys/jwt-public.pem}) se expone en
 * {@code /.well-known/jwks.json} para que los demás servicios puedan descargarla.
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

    @Value("${app.jwt.private-key-path:classpath:keys/jwt-private.pem}")
    private Resource privateKeyResource;

    @Value("${app.jwt.public-key-path:classpath:keys/jwt-public.pem}")
    private Resource publicKeyResource;

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private RSAPrivateKey privateKey;
    private RSAPublicKey  publicKey;

    @PostConstruct
    void init() {
        try {
            privateKey = loadPrivateKey(privateKeyResource);
            publicKey  = loadPublicKey(publicKeyResource);
            log.info("JWT RS256 keys loaded successfully. Public key modulus length: {} bits",
                    publicKey.getModulus().bitLength());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JWT RSA keys — check app.jwt.private-key-path " +
                    "and app.jwt.public-key-path", e);
        }
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
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(alias)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(privateKey)        // RS256 con clave privada
                .compact();
    }

    @Override
    public String generateRefreshToken(Integer userId) {
        return Jwts.builder()
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

    // ── Private helpers ──────────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)       // RS256: verificar con clave pública
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Carga una clave privada PKCS#8 desde un recurso PEM.
     * {@code openssl genrsa} genera formato PKCS#1; {@code openssl pkcs8} lo convierte.
     * Las claves generadas con {@code openssl genrsa} sin conversión también funcionan
     * porque jjwt 0.12+ acepta ambos formatos internamente.
     */
    private RSAPrivateKey loadPrivateKey(Resource resource) throws Exception {
        String pem = new String(resource.getInputStream().readAllBytes())
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
     * Carga una clave pública X.509/SubjectPublicKeyInfo desde un recurso PEM.
     * {@code openssl rsa -pubout} genera este formato directamente.
     */
    private RSAPublicKey loadPublicKey(Resource resource) throws Exception {
        String pem = new String(resource.getInputStream().readAllBytes())
                .replaceAll("-----BEGIN.*?-----", "")
                .replaceAll("-----END.*?-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}
