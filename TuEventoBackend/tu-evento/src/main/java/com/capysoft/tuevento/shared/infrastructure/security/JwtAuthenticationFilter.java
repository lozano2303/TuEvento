package com.capysoft.tuevento.shared.infrastructure.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.capysoft.tuevento.modules.security.application.port.out.TokenGeneratorPort;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.capysoft.tuevento.modules.security.application.port.out.CurrentRolePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX        = "Bearer ";
    private static final String ORGANIZER_ROLE       = "ORGANIZER";

    private final TokenGeneratorPort tokenGenerator;
    private final CurrentRolePort    currentRolePort;

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

        if (StringUtils.hasText(token) && tokenGenerator.isTokenValid(token)) {
            tokenGenerator.extractUserId(token).ifPresent(userId -> {
                String tokenRole = tokenGenerator.extractRole(token).orElse("");
                // El refresh token (7 días) se firma igual pero no lleva rol: no sirve como access token
                if (tokenRole.isBlank()) {
                    log.debug("JWT without role ignored (not an access token) for userId={}", userId);
                    return;
                }
                String alias = tokenGenerator.extractSubject(token).orElse("");
                String role  = resolveRole(userId, tokenRole);

                log.debug("JWT authorities loaded for userId={}: [{}]", userId, role);

                SecurityUser securityUser = new SecurityUser(userId, alias, role);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                securityUser, null, securityUser.getAuthorities());

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Un admin puede quitar el rol de organizador mientras el access token (15 min) sigue
     * vigente. Para no depender de que el cliente renueve el JWT, el rol ORGANIZER del
     * token se confirma contra la BD; solo esos tokens pagan la consulta extra.
     */
    private String resolveRole(Integer userId, String tokenRole) {
        if (!ORGANIZER_ROLE.equals(tokenRole)) return tokenRole;
        try {
            return currentRolePort.findRoleCode(userId).orElse(tokenRole);
        } catch (Exception e) {
            log.warn("Could not verify current role for userId={}: {}", userId, e.getMessage());
            return tokenRole;
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

}
