package com.briefen.controller;

import com.briefen.dto.UserSettingsDto;
import com.briefen.exception.InvalidUrlException;
import com.briefen.model.UserSettings;
import com.briefen.repository.UserSettingsRepository;
import com.briefen.validation.UrlValidator;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final UserSettingsRepository repository;
    private final UrlValidator urlValidator;

    public SettingsController(UserSettingsRepository repository, UrlValidator urlValidator) {
        this.repository = repository;
        this.urlValidator = urlValidator;
    }

    @GetMapping
    public UserSettingsDto get() {
        UserSettings settings = repository.findById(UserSettings.DEFAULT_ID)
                .orElseGet(UserSettings::new);
        return UserSettingsDto.fromMasked(settings);
    }

    @PutMapping
    public UserSettingsDto update(@RequestBody UserSettingsDto dto) {
        UserSettings settings = repository.findById(UserSettings.DEFAULT_ID)
                .orElseGet(UserSettings::new);

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

        return UserSettingsDto.fromMasked(repository.save(settings));
    }
}
