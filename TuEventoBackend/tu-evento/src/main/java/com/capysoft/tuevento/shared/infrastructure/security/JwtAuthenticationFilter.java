package com.capysoft.tuevento.shared.infrastructure.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.capysoft.tuevento.modules.security.application.dto.AccessTokenClaims;
import com.capysoft.tuevento.modules.security.application.port.out.TokenGeneratorPort;
import com.capysoft.tuevento.modules.security.application.port.out.TokenGeneratorPort.TokenMetadata;
import com.capysoft.tuevento.modules.security.application.port.out.TokenRevocationPort;
import com.capysoft.tuevento.modules.security.application.port.out.TokenRevocationPort.TokenRevocationUnavailableException;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Autentica la petición con el access token del header {@code Authorization}.
 *
 * <p>El rol sale del token, sin consultar la BD. Lo que cambia el acceso mientras un token sigue
 * vigente (revocar el rol de organizador, desactivar la cuenta, cerrar sesión) lo registra en
 * {@link TokenRevocationPort}; un token revocado se trata como si no viniera (401 en los
 * endpoints protegidos) y el cliente renueva y recibe el rol actual.
 *
 * <p>Fail-closed: si no se puede consultar la lista de revocación, la petición con token se
 * corta con 503 en vez de aceptarla a ciegas.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX        = "Bearer ";

    private final TokenGeneratorPort  tokenGenerator;
    private final TokenRevocationPort tokenRevocationPort;
    private final ObjectMapper        objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") || path.startsWith("/api/v1/webhooks/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);
        AccessTokenClaims claims = StringUtils.hasText(token) ? readAccessToken(token) : null;

        if (claims != null) {
            boolean revoked;
            try {
                revoked = tokenRevocationPort.isRevoked(claims);
            } catch (TokenRevocationUnavailableException e) {
                log.error("Token revocation store unavailable, rejecting request for userId={}: {}",
                        claims.userId(), e.getMessage());
                rejectUnavailable(response);
                return;
            }
            if (revoked) {
                log.debug("Revoked JWT ignored for userId={}", claims.userId());
            } else {
                authenticate(request, claims);
            }
        }

        filterChain.doFilter(request, response);
    }

    /** Los claims de un access token válido, o null si no lo es (inválido, expirado o refresh token). */
    private AccessTokenClaims readAccessToken(String token) {
        if (!tokenGenerator.isTokenValid(token)) {
            return null;
        }
        Integer userId = tokenGenerator.extractUserId(token).orElse(null);
        String role = tokenGenerator.extractRole(token).orElse("");
        // El refresh token (7 días) se firma igual pero no lleva rol: no sirve como access token
        if (userId == null || role.isBlank()) {
            log.debug("JWT without userId or role ignored (not an access token)");
            return null;
        }
        String alias = tokenGenerator.extractSubject(token).orElse("");
        TokenMetadata metadata = tokenGenerator.extractMetadata(token).orElse(null);
        return new AccessTokenClaims(userId, alias, role,
                metadata == null ? null : metadata.tokenId(),
                metadata == null ? null : metadata.issuedAt(),
                metadata == null ? null : metadata.expiresAt());
    }

    private void authenticate(HttpServletRequest request, AccessTokenClaims claims) {
        log.debug("JWT authorities loaded for userId={}: [{}]", claims.userId(), claims.role());
        SecurityUser securityUser = new SecurityUser(claims.userId(), claims.alias(), claims.role());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void rejectUnavailable(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error("AUTH_UNAVAILABLE",
                "No se pudo validar la sesión. Intenta de nuevo en unos segundos."));
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

}
