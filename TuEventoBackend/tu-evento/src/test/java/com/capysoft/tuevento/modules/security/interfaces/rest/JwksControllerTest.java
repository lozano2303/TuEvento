package com.capysoft.tuevento.modules.security.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.capysoft.tuevento.modules.security.infrastructure.external.JwtTokenGenerator;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwksController — /.well-known/jwks.json")
class JwksControllerTest {

    @Mock JwtTokenGenerator jwtTokenGenerator;
    @InjectMocks JwksController controller;

    @Test
    @DisplayName("publica la clave pública RSA con el kid de los tokens, sin el byte de signo")
    @SuppressWarnings("unchecked")
    void publishesPublicKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        RSAPublicKey publicKey = (RSAPublicKey) generator.generateKeyPair().getPublic();
        when(jwtTokenGenerator.getPublicKey()).thenReturn(publicKey);

        Map<String, Object> jwk = ((List<Map<String, Object>>) controller.jwks().get("keys")).get(0);

        assertThat(jwk).containsEntry("kty", "RSA").containsEntry("alg", "RS256").containsEntry("use", "sig")
                .containsEntry("kid", JwtTokenGenerator.KEY_ID);
        byte[] modulus = Base64.getUrlDecoder().decode((String) jwk.get("n"));
        assertThat(modulus).hasSize(256);
        assertThat(new BigInteger(1, modulus)).isEqualTo(publicKey.getModulus());
        assertThat(new BigInteger(1, Base64.getUrlDecoder().decode((String) jwk.get("e"))))
                .isEqualTo(publicKey.getPublicExponent());
    }
}
