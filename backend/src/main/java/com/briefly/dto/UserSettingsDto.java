package com.briefly.dto;

import com.briefly.model.UserSettings;

public record UserSettingsDto(
        String defaultLength,
        String model
) {
    public static UserSettingsDto from(UserSettings entity) {
        return new UserSettingsDto(entity.getDefaultLength(), entity.getModel());
    }
}
