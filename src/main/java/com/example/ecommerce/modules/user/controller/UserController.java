package com.example.ecommerce.modules.user.controller;

import com.example.ecommerce.common.api.ApiResponse;
import com.example.ecommerce.common.auth.LoginRequired;
import com.example.ecommerce.modules.user.dto.LoginRequest;
import com.example.ecommerce.modules.user.dto.LoginResponse;
import com.example.ecommerce.modules.user.dto.RegisterRequest;
import com.example.ecommerce.modules.user.dto.UserProfileResponse;
import com.example.ecommerce.modules.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(userService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(userService.login(request));
    }

    @LoginRequired
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me() {
        return ApiResponse.ok(userService.me());
    }
}
