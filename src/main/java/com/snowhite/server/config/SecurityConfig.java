package com.snowhite.server.config;

import com.snowhite.server.security.filter.JwtWebFilter;
import com.snowhite.server.security.handler.CustomAccessDeniedHandler;
import com.snowhite.server.security.handler.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtWebFilter jwtWebFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(
                        authorize -> authorize
                                .pathMatchers(
                                        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                        "/swagger-resources/**", "/webjars/**",
                                        "/users/login", "/users/register", "/users/check-email","/users/check-login",
                                        "/lobby", "/rooms"
                                ).permitAll()
                                .anyExchange().authenticated()
                )
                .addFilterBefore(jwtWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(customAccessDeniedHandler)
                        .authenticationEntryPoint(customAuthenticationEntryPoint));

        return http.build();
    }

}