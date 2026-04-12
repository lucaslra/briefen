package com.briefen.persistence.jpa;

import com.briefen.model.UserSettings;
import com.briefen.persistence.SettingsPersistence;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@Transactional
public class JpaSettingsPersistence implements SettingsPersistence {

    private final JpaUserSettingsRepository repository;

    public JpaSettingsPersistence(JpaUserSettingsRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSettings> findByUserId(String userId) {
        return repository.findById(userId)
                .map(JpaUserSettingsEntity::toDomain);
    }

    @Override
    public UserSettings save(UserSettings settings) {
        var entity = JpaUserSettingsEntity.fromDomain(settings);
        return repository.save(entity).toDomain();
    }
}
