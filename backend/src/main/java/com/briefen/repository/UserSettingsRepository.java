package com.briefen.repository;

import com.briefen.model.UserSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserSettingsRepository extends MongoRepository<UserSettings, String> {
}
