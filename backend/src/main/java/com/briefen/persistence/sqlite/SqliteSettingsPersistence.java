package com.briefen.persistence.sqlite;

import com.briefen.model.UserSettings;
import com.briefen.persistence.SettingsPersistence;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@Transactional
public class SqliteSettingsPersistence implements SettingsPersistence {

    private final SqliteUserSettingsRepository repository;

    public SqliteSettingsPersistence(SqliteUserSettingsRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSettings> findByUserId(String userId) {
        return repository.findById(userId)
                .map(SqliteUserSettingsEntity::toDomain);
    }

    @Override
    public UserSettings save(UserSettings settings) {
        var entity = SqliteUserSettingsEntity.fromDomain(settings);
        return repository.save(entity).toDomain();
    }
}
