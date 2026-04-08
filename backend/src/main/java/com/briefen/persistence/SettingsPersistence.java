package com.briefen.persistence;

import com.briefen.model.UserSettings;

import java.util.Optional;

public interface SettingsPersistence {

    Optional<UserSettings> findByUserId(String userId);

    UserSettings save(UserSettings settings);
}
