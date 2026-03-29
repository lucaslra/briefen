package com.briefen.dto;

import com.briefen.model.UserSettings;

public record UserSettingsDto(
        String defaultLength,
        String model
) {
    public static UserSettingsDto from(UserSettings entity) {
        return new UserSettingsDto(entity.getDefaultLength(), entity.getModel());
    }
}
