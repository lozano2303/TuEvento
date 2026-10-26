package com.capysoft.storage.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import com.capysoft.common.infrastructure.security.SecurityUser;

@DisplayName("SecurityConfig — JWT a SecurityUser")
class SecurityConfigTest {

    private static Jwt.Builder jwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("ana")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
    }

    @Test
    @DisplayName("el principal es un SecurityUser con userId, alias y rol del token")
    void toAuthentication_buildsSecurityUserFromClaims() {
        AbstractAuthenticationToken auth = SecurityConfig.toAuthentication(
                jwt().claim("userId", 42).claim("role", "ADMIN").build());

        assertThat(auth.getPrincipal()).isInstanceOf(SecurityUser.class);
        SecurityUser user = (SecurityUser) auth.getPrincipal();
        assertThat(user.getUserId()).isEqualTo(42);
        assertThat(user.getAlias()).isEqualTo("ana");
        assertThat(user.isAdmin()).isTrue();
        assertThat(auth.isAuthenticated()).isTrue();
        // Misma autoridad que en el monolito, sin prefijo ROLE_
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ADMIN");
    }

    @Test
    @DisplayName("un token sin rol queda autenticado pero sin autoridades")
    void toAuthentication_withoutRole_hasNoAuthorities() {
        AbstractAuthenticationToken auth = SecurityConfig.toAuthentication(jwt().claim("userId", 7).build());

        assertThat(((SecurityUser) auth.getPrincipal()).getUserId()).isEqualTo(7);
        assertThat(auth.getAuthorities()).isEmpty();
    }
}
