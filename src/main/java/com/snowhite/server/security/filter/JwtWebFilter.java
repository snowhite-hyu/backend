package com.snowhite.server.security.filter;

import com.snowhite.server.security.jwt.JwtProvider;
import com.snowhite.server.domain.entity.User;
import com.snowhite.server.repository.UserRepository;
import com.snowhite.server.security.model.CustomUserDetails;
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

import java.util.Collections;
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
            "/users/login", "/users/register", "/users/check-email","/users/check-login"
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
                String userIdStr = jwtProvider.extractClaim(token, Claims::getId);
                Long userId = Long.parseLong(userIdStr);
                return Mono.fromCallable(() -> userRepository.findById(userId))
                        .flatMap(optionalUser -> {
                            if (optionalUser.isEmpty()) {
                                return chain.filter(exchange);
                            }
                            User user = optionalUser.get();
                            CustomUserDetails customUserDetails = new CustomUserDetails(user);

                            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                                    customUserDetails,null, Collections.emptyList()
                            );
                            return chain.filter(exchange)
                                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authenticationToken));
                        });
            }
        }

        return chain.filter(exchange);
    }

    private boolean shouldExclude(String path) {
        return EXCLUDE_URLS.stream().anyMatch(path::startsWith);
    }
}
