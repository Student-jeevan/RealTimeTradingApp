package com.jeevan.TradingApp.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;

public class JwtTokenValidator extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Skip JWT validation for OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = request.getHeader(JwtConstant.jwt_header);
        if (jwt != null && jwt.startsWith("Bearer ")) {
            jwt = jwt.substring(7);
            try {
                SecretKey key = Keys.hmacShaKeyFor(JwtConstant.secret_key.getBytes());
                Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwt).getBody();
                String email = String.valueOf(claims.get("email"));
                String authorities = String.valueOf(claims.get("authorities"));
                List<GrantedAuthority> authoritiesList = AuthorityUtils
                        .commaSeparatedStringToAuthorityList(authorities);
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        authoritiesList);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                // Log error but do not throw exception to allow anonymous access for public
                // endpoints
                // throw new RuntimeException("invalid token ... .");
            }
        }
        filterChain.doFilter(request, response);
    }
}
