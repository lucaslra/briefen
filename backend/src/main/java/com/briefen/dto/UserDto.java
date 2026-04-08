package com.briefen.dto;

import com.briefen.model.User;

import java.time.Instant;

public record UserDto(String id, String username, String role, Instant createdAt, boolean mainAdmin) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getRole(), user.getCreatedAt(), user.isMainAdmin());
    }
}
