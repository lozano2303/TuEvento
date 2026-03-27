package com.capysoft.tuevento.shared.infrastructure.security;

import com.capysoft.tuevento.modules.security.application.port.out.TokenGeneratorPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX        = "Bearer ";

    private final TokenGeneratorPort tokenGenerator;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (StringUtils.hasText(token) && tokenGenerator.isTokenValid(token)) {
            tokenGenerator.extractUserId(token).ifPresent(userId -> {
                // userId is present — build SecurityUser from token claims
                // Role is embedded in the token; we re-extract via a lightweight parse
                String alias = extractClaim(token, "sub");
                String role  = extractClaim(token, "role");

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

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * Lightweight claim extraction without full validation (already validated above).
     * Decodes the JWT payload (Base64) and extracts the requested field.
     */
    private String extractClaim(String token, String claimName) {
        try {
            String payload = token.split("\\.")[1];
            String decoded = new String(java.util.Base64.getUrlDecoder().decode(payload));
            // Simple JSON field extraction — avoids pulling in a full JSON parser here
            String search = "\"" + claimName + "\":\"";
            int start = decoded.indexOf(search);
            if (start == -1) return null;
            start += search.length();
            int end = decoded.indexOf("\"", start);
            return decoded.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }
}
