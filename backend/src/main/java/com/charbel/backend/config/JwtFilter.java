package com.charbel.backend.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.charbel.backend.service.JWTService;
import com.charbel.backend.service.MyUserDetailsService;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

import org.springframework.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);
    private static final String COOKIE_NAME = "jwt_token";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer";

    private final JWTService jwtService;
    private final MyUserDetailsService userDetailsService;

    public JwtFilter(JWTService jwtService, MyUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    private String resolveToken(HttpServletRequest request) {
        if(request.getCookies() != null) {
            for(Cookie c : request.getCookies()) {
                if(COOKIE_NAME.equals(c.getName())){
                    return c.getValue();
                }
            }
        }

        String auth = request.getHeader(AUTH_HEADER);
        if(auth != null && auth.startsWith(BEARER_PREFIX)) {
            return auth.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain) throws ServletException, IOException {
        if(SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = resolveToken(request);

            if(token != null && !token.isBlank()) {
                try {
                    String username = jwtService.extractEmail(token);
                    if(username != null && jwtService.isTokenValid(token, username)) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        var authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
                catch (ExpiredJwtException ex) {
                    log.debug("JWT expiré: {}", ex.getMessage());
                }
                catch (JwtException | IllegalArgumentException ex) {
                    log.debug("JWT invalide: {}", ex.getMessage());
                }
                catch (Exception ex) {
                    log.warn("Erreur durant l'authentification JWT: {}", ex.getMessage());
                }
            }
        }

        chain.doFilter(request, response);
    }
}