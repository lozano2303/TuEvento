package com.capysoft.tuevento.modules.security.interfaces.rest;

import com.capysoft.tuevento.modules.security.infrastructure.external.JwtTokenGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Expone la clave pública RSA en formato JWK Set (RFC 7517).
 *
 * <p>Endpoint: {@code GET /.well-known/jwks.json}
 *
 * <p>Este endpoint es consumido por:
 * <ul>
 *   <li>El futuro Spring Cloud Gateway para validar JWT sin llamar al servicio Identidad.
 *   <li>Cada microservicio configurado como resource server con {@code jwk-set-uri}.
 *   <li>Herramientas de debugging como jwt.io.
 * </ul>
 *
 * <p>La respuesta sigue el estándar JWK Set:
 * <pre>
 * {
 *   "keys": [{
 *     "kty": "RSA",
 *     "use": "sig",
 *     "alg": "RS256",
 *     "kid": "tuevento-jwt-key-1",
 *     "n":   "&lt;modulus en Base64URL&gt;",
 *     "e":   "&lt;exponent en Base64URL&gt;"
 *   }]
 * }
 * </pre>
 *
 * <p>El endpoint es público (configurado en {@code SecurityConfig.PUBLIC_ENDPOINTS}).
 * No expone la clave privada — solo el módulo y el exponente públicos.
 */
@RestController
@RequiredArgsConstructor
public class JwksController {

    /** ID de la clave — permite rotar claves sin romper tokens en vuelo. */
    private static final String KEY_ID = "tuevento-jwt-key-1";

    private final JwtTokenGenerator jwtTokenGenerator;

    /**
     * Devuelve el JWK Set con la clave pública RSA activa.
     *
     * <p>El {@code Content-Type} es {@code application/json}. Algunos clientes
     * esperan {@code application/jwk-set+json} (RFC 7517 §8.5), pero Spring
     * Boot lo mapea igualmente con {@code @RestController}.
     */
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        RSAPublicKey publicKey = jwtTokenGenerator.getPublicKey();

        // Codificar módulo y exponente en Base64URL sin padding (estándar JWK)
        String n = base64UrlEncode(publicKey.getModulus());
        String e = base64UrlEncode(publicKey.getPublicExponent());

        Map<String, Object> jwk = Map.of(
                "kty", "RSA",
                "use", "sig",
                "alg", "RS256",
                "kid", KEY_ID,
                "n",   n,
                "e",   e
        );

        return Map.of("keys", List.of(jwk));
    }

    /**
     * Codifica un {@link BigInteger} (módulo o exponente RSA) en Base64URL sin padding,
     * respetando el requisito de bytes sin signo del estándar JWK (RFC 7518 §6.3.1).
     */
    private String base64UrlEncode(BigInteger value) {
        byte[] bytes = value.toByteArray();
        // BigInteger.toByteArray() puede incluir un byte 0x00 inicial para números positivos
        // con el bit más significativo en 1. JWK requiere la representación mínima sin ese byte.
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            bytes = trimmed;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
