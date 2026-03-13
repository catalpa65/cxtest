package com.example.ecommerce.common.auth;

public record UserPrincipal(Long userId, String email, String nickname) {
}
