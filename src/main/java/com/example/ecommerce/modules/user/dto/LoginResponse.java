package com.example.ecommerce.modules.user.dto;

public record LoginResponse(String token, long expiresInSeconds, UserProfileResponse user) {
}
