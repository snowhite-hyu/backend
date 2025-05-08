package com.snowhite.server.service;

import com.snowhite.server.domain.User;
import com.snowhite.server.domain.UserRepository;
import com.snowhite.server.payload.ApiResponse;
import com.snowhite.server.payload.code.status.ErrorStatus;
import com.snowhite.server.dto.EmailDto;
import com.snowhite.server.dto.RegisterDto;
import com.snowhite.server.payload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Transactional
    public Mono<ApiResponse<String>> register(final RegisterDto registerDto){
        if (checkEmail(registerDto.getEmail())) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }
        User user = new User();
        user.setEmail(registerDto.getEmail());
        user.setUsername(registerDto.getUsername());
        user.setPassword(bCryptPasswordEncoder.encode(registerDto.getPassword()));
        userRepository.save(user);
        return Mono.just(ApiResponse.onSuccess("회원가입이 완료되었습니다."));
    }

    public Mono<ApiResponse<String>> checkEmail(final EmailDto emailDto) {
        boolean exists = checkEmail(emailDto.getEmail());
        String message = exists ? "사용 중인 이메일입니다." : "사용 가능한 이메일입니다.";
        return Mono.just(ApiResponse.onSuccess(message));
    }

    private boolean checkEmail(String email) {
        return userRepository.findByEmail(email) != null;
    }
}
