package com.briefen.controller;

import com.briefen.dto.UserSettingsDto;
import com.briefen.exception.InvalidUrlException;
import com.briefen.model.UserSettings;
import com.briefen.persistence.SettingsPersistence;
import com.briefen.security.BriefenUserDetails;
import com.briefen.validation.UrlValidator;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsPersistence settingsPersistence;
    private final UrlValidator urlValidator;

    public SettingsController(SettingsPersistence settingsPersistence, UrlValidator urlValidator) {
        this.settingsPersistence = settingsPersistence;
        this.urlValidator = urlValidator;
    }

    @GetMapping
    public UserSettingsDto get(@AuthenticationPrincipal BriefenUserDetails userDetails) {
        String userId = userDetails.userId();
        UserSettings settings = settingsPersistence.findByUserId(userId)
                .orElseGet(() -> {
                    var s = new UserSettings();
                    s.setId(userId);
                    return s;
                });
        return UserSettingsDto.fromMasked(settings);
    }

    @PutMapping
    public UserSettingsDto update(
            @AuthenticationPrincipal BriefenUserDetails userDetails,
            @RequestBody UserSettingsDto dto) {
        String userId = userDetails.userId();
        UserSettings settings = settingsPersistence.findByUserId(userId)
                .orElseGet(() -> {
                    var s = new UserSettings();
                    s.setId(userId);
                    return s;
                });

        if (dto.defaultLength() != null) {
            settings.setDefaultLength(dto.defaultLength());
        }
        if (dto.model() != null) {
            settings.setModel(dto.model());
        }
        if (dto.notificationsEnabled() != null) {
            settings.setNotificationsEnabled(dto.notificationsEnabled());
        }
        if (dto.openaiApiKey() != null) {
            settings.setOpenaiApiKey(dto.openaiApiKey().isBlank() ? null : dto.openaiApiKey());
        }
        if (dto.anthropicApiKey() != null) {
            settings.setAnthropicApiKey(dto.anthropicApiKey().isBlank() ? null : dto.anthropicApiKey());
        }
        if (dto.readeckApiKey() != null) {
            settings.setReadeckApiKey(dto.readeckApiKey().isBlank() ? null : dto.readeckApiKey());
        }
        if (dto.readeckUrl() != null) {
            if (dto.readeckUrl().isBlank()) {
                settings.setReadeckUrl(null);
            } else {
                try {
                    urlValidator.validateServiceUrl(dto.readeckUrl());
                    settings.setReadeckUrl(dto.readeckUrl());
                } catch (InvalidUrlException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid Readeck URL: " + e.getMessage());
                }
            }
        }
        if (dto.webhookUrl() != null) {
            if (dto.webhookUrl().isBlank()) {
                settings.setWebhookUrl(null);
            } else {
                try {
                    urlValidator.validateServiceUrl(dto.webhookUrl());
                    settings.setWebhookUrl(dto.webhookUrl());
                } catch (InvalidUrlException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid webhook URL: " + e.getMessage());
                }
            }
        }
        if (dto.customPrompt() != null) {
            settings.setCustomPrompt(dto.customPrompt().isBlank() ? null : dto.customPrompt());
        }

        return UserSettingsDto.fromMasked(settingsPersistence.save(settings));
    }
}
