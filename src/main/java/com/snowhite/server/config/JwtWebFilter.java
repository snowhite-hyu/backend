package com.snowhite.server.config;

import com.snowhite.server.domain.User;
import com.snowhite.server.domain.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtWebFilter implements WebFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    private static final List<String> EXCLUDE_URLS = List.of(
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
            "/swagger-resources/**", "/webjars/**",
            "/login", "/register", "/checkname"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (shouldExclude(path)) {
            return chain.filter(exchange);
        }

        String accessToken = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            String token = accessToken.substring(7);
            if (jwtProvider.isTokenValid(token)) {
                String userId = jwtProvider.extractClaim(token, Claims::getId);
                Optional<User> user = userRepository.findById(Long.parseLong(userId));
                var authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        null
                );
                return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
            }
        }

        return chain.filter(exchange);
    }

    private boolean shouldExclude(String path) {
        return EXCLUDE_URLS.stream().anyMatch(path::startsWith);
    }
}
