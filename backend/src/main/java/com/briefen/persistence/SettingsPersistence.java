package com.briefen.persistence;

import com.briefen.model.UserSettings;

import java.util.Optional;

public interface SettingsPersistence {

    Optional<UserSettings> findDefault();

    UserSettings save(UserSettings settings);
}
