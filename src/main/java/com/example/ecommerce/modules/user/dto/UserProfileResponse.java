package com.example.ecommerce.modules.user.dto;

import com.example.ecommerce.modules.user.model.User;

public record UserProfileResponse(Long id, String email, String nickname) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getNickname());
    }
}
