package com.briefen.persistence.sqlite;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SqliteUserSettingsRepository extends JpaRepository<SqliteUserSettingsEntity, String> {
}
