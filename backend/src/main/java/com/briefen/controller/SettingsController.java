package com.briefen.controller;

import com.briefen.dto.UserSettingsDto;
import com.briefen.model.UserSettings;
import com.briefen.repository.UserSettingsRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final UserSettingsRepository repository;

    public SettingsController(UserSettingsRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public UserSettingsDto get() {
        UserSettings settings = repository.findById(UserSettings.DEFAULT_ID)
                .orElseGet(UserSettings::new);
        return UserSettingsDto.from(settings);
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
        settings.setNotificationsEnabled(dto.notificationsEnabled());
        if (dto.openaiApiKey() != null) {
            settings.setOpenaiApiKey(dto.openaiApiKey().isBlank() ? null : dto.openaiApiKey());
        }

        return UserSettingsDto.from(repository.save(settings));
    }
}
