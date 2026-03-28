package com.briefly.dto;

import com.briefly.model.UserSettings;

public record UserSettingsDto(
        String defaultLength
) {
    public static UserSettingsDto from(UserSettings entity) {
        return new UserSettingsDto(entity.getDefaultLength());
    }
}
