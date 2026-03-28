package com.briefly.repository;

import com.briefly.model.UserSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserSettingsRepository extends MongoRepository<UserSettings, String> {
}
