package com.capysoft.tuevento.modules.security.infrastructure.external;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import io.jsonwebtoken.security.Keys;
import org.springframework.core.io.ClassPathResource;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifica que JwtTokenGenerator firma con RS256 y que los tokens son únicos (jti).
 *
 * <p>El par de claves RSA se genera en memoria para no depender de archivos en disco.
 * Se inyectan como {@code ByteArrayResource} en formato PEM para que el código de
 * producción los lea exactamente igual que si viniera del classpath.
 */
@DisplayName("JwtTokenGenerator — RS256")
class JwtTokenGeneratorTest {

    private JwtTokenGenerator generator;
    private RSAPublicKey       testPublicKey;

    @BeforeEach
    void setUp() throws Exception {
        // Generar par RSA-2048 en memoria para los tests
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair pair = kpg.generateKeyPair();

        RSAPrivateKey privateKey = (RSAPrivateKey) pair.getPrivate();
        testPublicKey = (RSAPublicKey) pair.getPublic();

        // Serializar a PEM para que JwtTokenGenerator los cargue igual que en producción
        String privatePem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(privateKey.getEncoded())
                + "\n-----END PRIVATE KEY-----\n";

        generator = new JwtTokenGenerator();
        ReflectionTestUtils.setField(generator, "privateKeyResource",
                new ByteArrayResource(privatePem.getBytes()));
        ReflectionTestUtils.setField(generator, "accessTokenExpiration",  900_000L);
        ReflectionTestUtils.setField(generator, "refreshTokenExpiration", 604_800_000L);
        generator.init();
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(testPublicKey)   // RS256: verificar con la clave pública del test
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Test
    @DisplayName("dos access tokens para el mismo usuario en el mismo segundo son distintos (jti)")
    void accessTokens_sameSecond_areUnique() {
        String first;
        String second;
        int attempts = 0;
        do {
            first  = generator.generateAccessToken(42, "alias", "USER");
            second = generator.generateAccessToken(42, "alias", "USER");
        } while (!claims(first).getIssuedAt().equals(claims(second).getIssuedAt()) && ++attempts < 5);

        assertThat(claims(first).getIssuedAt()).isEqualTo(claims(second).getIssuedAt());
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("dos refresh tokens para el mismo usuario son distintos (jti)")
    void refreshTokens_sameSecond_areUnique() {
        assertThat(generator.generateRefreshToken(42))
                .isNotEqualTo(generator.generateRefreshToken(42));
    }

    @Test
    @DisplayName("los tokens llevan jti UUID y conservan los claims originales")
    void tokens_carryUuidJti_andClaims() {
        String access  = generator.generateAccessToken(42, "alias", "ORGANIZER");
        String refresh = generator.generateRefreshToken(42);

        assertThatCode(() -> UUID.fromString(claims(access).getId())).doesNotThrowAnyException();
        assertThatCode(() -> UUID.fromString(claims(refresh).getId())).doesNotThrowAnyException();

        assertThat(generator.extractUserId(access)).contains(42);
        assertThat(generator.extractRole(access)).contains("ORGANIZER");
        assertThat(generator.extractSubject(access)).contains("alias");
        assertThat(generator.isTokenValid(refresh)).isTrue();
    }

    @Test
    @DisplayName("getPublicKey devuelve la clave pública RSA cargada")
    void getPublicKey_returnsLoadedKey() {
        assertThat(generator.getPublicKey()).isNotNull();
        assertThat(generator.getPublicKey().getAlgorithm()).isEqualTo("RSA");
        assertThat(generator.getPublicKey().getModulus().bitLength()).isGreaterThanOrEqualTo(2048);
    }

    @Test
    @DisplayName("un token con firma inválida no pasa la validación")
    void invalidToken_isRejected() {
        String valid = generator.generateAccessToken(1, "user", "USER");
        // Corromper la firma (último segmento del JWT)
        String[] parts = valid.split("\\.");
        String tampered = parts[0] + "." + parts[1] + ".invalidsignature";
        assertThat(generator.isTokenValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("la clave pública se deriva de la privada y los tokens llevan el kid del JWKS")
    void publicKey_isDerived_andTokensCarryKid() {
        assertThat(generator.getPublicKey().getModulus()).isEqualTo(testPublicKey.getModulus());
        String token = generator.generateAccessToken(1, "user", "USER");
        assertThat(Jwts.parser().verifyWith(testPublicKey).build().parseSignedClaims(token).getHeader().getKeyId())
                .isEqualTo(JwtTokenGenerator.KEY_ID);
    }

    @Test
    @DisplayName("sin secreto legado, un token HS256 se rechaza")
    void legacyHs256Token_rejectedWithoutSecret() {
        assertThat(generator.isTokenValid(hs256Token(LEGACY_SECRET))).isFalse();
    }

    @Test
    @DisplayName("con secreto legado, un token HS256 anterior sigue siendo válido")
    void legacyHs256Token_acceptedWithSecret() {
        ReflectionTestUtils.setField(generator, "legacySecret", LEGACY_SECRET);
        generator.init();

        String legacy = hs256Token(LEGACY_SECRET);
        assertThat(generator.isTokenValid(legacy)).isTrue();
        assertThat(generator.extractUserId(legacy)).contains(7);
        assertThat(generator.isTokenValid(hs256Token("otro-secreto-distinto-de-al-menos-32-chars"))).isFalse();
        // Los tokens nuevos siguen siendo RS256
        assertThat(generator.isTokenValid(generator.generateAccessToken(1, "user", "USER"))).isTrue();
    }

    @Test
    @DisplayName("sin clave privada y con claves efímeras permitidas, genera un par en memoria")
    void missingKey_withEphemeralAllowed_generatesKeyPair() {
        JwtTokenGenerator dev = generatorWithMissingKey(true);
        dev.init();
        String token = dev.generateAccessToken(3, "dev", "USER");
        assertThat(dev.isTokenValid(token)).isTrue();
        assertThat(dev.getPublicKey().getModulus().bitLength()).isGreaterThanOrEqualTo(2048);
    }

    @Test
    @DisplayName("sin clave privada y sin claves efímeras, el arranque falla")
    void missingKey_withoutEphemeral_failsFast() {
        JwtTokenGenerator prod = generatorWithMissingKey(false);
        assertThatThrownBy(prod::init).isInstanceOf(IllegalStateException.class);
    }

    private static final String LEGACY_SECRET = "test-secret-key-for-jenkins-ci-only-32chars";

    private static String hs256Token(String secret) {
        return Jwts.builder()
                .claim("userId", 7)
                .subject("legacy")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }

    private static JwtTokenGenerator generatorWithMissingKey(boolean ephemeralAllowed) {
        JwtTokenGenerator g = new JwtTokenGenerator();
        ReflectionTestUtils.setField(g, "privateKeyResource", new ClassPathResource("keys/does-not-exist.pem"));
        ReflectionTestUtils.setField(g, "ephemeralKeysAllowed", ephemeralAllowed);
        ReflectionTestUtils.setField(g, "accessTokenExpiration", 900_000L);
        ReflectionTestUtils.setField(g, "refreshTokenExpiration", 604_800_000L);
        return g;
    }
}
