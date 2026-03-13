package com.example.ecommerce.modules.user.service;

import com.example.ecommerce.common.auth.AuthContext;
import com.example.ecommerce.common.auth.JwtTokenProvider;
import com.example.ecommerce.common.auth.UserPrincipal;
import com.example.ecommerce.common.error.ErrorCode;
import com.example.ecommerce.common.exception.BizException;
import com.example.ecommerce.modules.user.dto.LoginRequest;
import com.example.ecommerce.modules.user.dto.LoginResponse;
import com.example.ecommerce.modules.user.dto.RegisterRequest;
import com.example.ecommerce.modules.user.dto.UserProfileResponse;
import com.example.ecommerce.modules.user.model.User;
import com.example.ecommerce.modules.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public UserProfileResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BizException(ErrorCode.CONFLICT, "该邮箱已注册");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname().trim());
        return UserProfileResponse.from(userRepository.save(user));
    }

    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED, "邮箱或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "邮箱或密码错误");
        }

        String token = jwtTokenProvider.createToken(user);
        return new LoginResponse(token, jwtTokenProvider.getExpireSeconds(), UserProfileResponse.from(user));
    }

    public UserProfileResponse me() {
        UserPrincipal principal = AuthContext.get();
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "未登录");
        }

        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED, "用户不存在或登录态已失效"));
        return UserProfileResponse.from(user);
    }
}
