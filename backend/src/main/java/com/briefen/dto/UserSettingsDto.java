package com.briefen.dto;

import com.briefen.model.UserSettings;

public record UserSettingsDto(
        String defaultLength,
        String model,
        Boolean notificationsEnabled,
        String openaiApiKey,
        String readeckApiKey,
        String readeckUrl
) {
    /**
     * Returns a response-safe DTO with API keys masked (e.g., "sk-...abc1").
     * The frontend only needs to know if a key is set, not its full value.
     */
    public static UserSettingsDto fromMasked(UserSettings entity) {
        return new UserSettingsDto(
                entity.getDefaultLength(),
                entity.getModel(),
                entity.isNotificationsEnabled(),
                maskKey(entity.getOpenaiApiKey()),
                maskKey(entity.getReadeckApiKey()),
                entity.getReadeckUrl()
        );
    }

    private static String maskKey(String key) {
        if (key == null || key.isBlank()) return null;
        if (key.length() <= 8) return "****";
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
}
