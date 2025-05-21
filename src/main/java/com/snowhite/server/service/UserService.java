package com.snowhite.server.service;

import com.snowhite.server.domain.entity.User;
import com.snowhite.server.repository.UserRepository;
import com.snowhite.server.security.jwt.JwtProvider;
import com.snowhite.server.web.dto.web.request.LoginRequestDto;
import com.snowhite.server.web.dto.web.response.LoginResponseDto;
import com.snowhite.server.payload.code.status.ErrorStatus;
import com.snowhite.server.web.dto.web.request.EmailDto;
import com.snowhite.server.web.dto.web.request.RegisterDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtProvider jwtProvider;

    public Mono<String> login(final LoginRequestDto loginRequestDto) {
        User user = userRepository.findByEmail(loginRequestDto.getEmail());
        if(!bCryptPasswordEncoder.matches(loginRequestDto.getPassword(), user.getPassword()))
            throw new BadCredentialsException(ErrorStatus._BAD_REQUEST.toString());
        String accessToken = jwtProvider.generateToken(user.getId());
        LoginResponseDto.builder().token(accessToken).build();
        user.setLoggedIn(true);
        userRepository.save(user);
        return Mono.just(accessToken);
    }

    public Mono<String> checkLogin(Authentication authentication, ServerHttpRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.just("비로그인 상태");
        }
        String username = authentication.getName();
        String token = resolveToken(request);
        if (token == null || !jwtProvider.isTokenValid(token)) {
            return Mono.just("비로그인 상태");
        }

        Map<String, LocalDateTime> tokenTimes = jwtProvider.extractTokenTimes(token);
        String message = String.format(
                "로그인된 사용자: %s\n토큰 발급 시간 (iat): %s\n토큰 만료 시간 (exp): %s\n현재 시간: %s",
                username,
                tokenTimes.get("issuedAt"),
                tokenTimes.get("expiration"),
                tokenTimes.get("now")
        );

        return Mono.just(message);
    }

    private String resolveToken(ServerHttpRequest request) {
        String bearer = request.getHeaders().getFirst("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    public Mono<Boolean> register(RegisterDto registerDto){
        if (!checkEmail(registerDto.getEmail())) {
            return Mono.just(false);
        }
        User user = new User();
        user.setEmail(registerDto.getEmail());
        user.setUsername(registerDto.getUsername());
        user.setPassword(bCryptPasswordEncoder.encode(registerDto.getPassword()));
        userRepository.save(user);
        return Mono.just(true);
    }

    public Mono<Boolean> checkEmail(final EmailDto emailDto) {
        return Mono.just(checkEmail(emailDto.getEmail()));
    }

    private boolean checkEmail(String email) {
        return userRepository.findByEmail(email) == null;
    }
}
