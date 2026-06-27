package com.codewithsubhra.money_tracker_backend.security.config;

import com.codewithsubhra.money_tracker_backend.security.SecurityProperties;
import com.codewithsubhra.money_tracker_backend.security.SessionAuthenticationFilter;
import com.codewithsubhra.money_tracker_backend.security.session.SessionService;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    private final SessionService sessionService;
    private final RestAuthenticationEntryPoint entryPoint;
    private final Environment environment;

    public SecurityConfig(SessionService sessionService, RestAuthenticationEntryPoint entryPoint,
            Environment environment) {
        this.sessionService = sessionService;
        this.entryPoint = entryPoint;
        this.environment = environment;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        boolean devProfile = Arrays.asList(environment.getActiveProfiles()).contains("dev");

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh.authenticationEntryPoint(entryPoint))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh").permitAll();
                    auth.requestMatchers("/actuator/health").permitAll();
                    auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    if (devProfile) {
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(new SessionAuthenticationFilter(sessionService),
                        UsernamePasswordAuthenticationFilter.class);

        // The H2 console renders inside a frame; relax the header only in dev.
        if (devProfile) {
            http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        }

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        String origins = environment.getProperty("app.cors.allowed-origins", "*");
        CorsConfiguration config = new CorsConfiguration();
        if ("*".equals(origins.trim())) {
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
