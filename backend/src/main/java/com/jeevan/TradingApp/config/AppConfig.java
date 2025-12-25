package com.jeevan.TradingApp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import java.util.Arrays;
import java.util.Collections;

@Configuration
public class AppConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(Authorize -> Authorize
                                                .requestMatchers("/api/**").authenticated()
                                                .requestMatchers("/auth/**").permitAll()
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .anyRequest().permitAll())
                                .addFilterBefore(new JwtTokenValidator(), BasicAuthenticationFilter.class)
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()));
                return http.build();
        }

        private CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration cfg = new CorsConfiguration();
                cfg.setAllowedOrigins(Arrays.asList(
                                "http://localhost:5173", // Vite dev server
                                "http://localhost:3001" // Docker mapped port
                ));
                cfg.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                cfg.setAllowedHeaders(Collections.singletonList("*"));
                cfg.setExposedHeaders(Arrays.asList("Authorization"));
                cfg.setAllowCredentials(true);
                cfg.setMaxAge(3600L);

                org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", cfg);
                return source;
        }

}
