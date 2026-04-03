package com.briefen.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@EnableJpaRepositories(basePackages = "com.briefen.persistence.sqlite")
public class SqliteConfig {

    @Bean
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.driver-class-name}") String driverClassName) throws IOException {
        // Extract file path from jdbc:sqlite:/path/to/file and ensure directory exists
        String filePath = url.replace("jdbc:sqlite:", "");
        int queryIndex = filePath.indexOf('?');
        if (queryIndex > 0) {
            filePath = filePath.substring(0, queryIndex);
        }
        Path dir = Path.of(filePath).getParent();
        if (dir != null) {
            Files.createDirectories(dir);
        }

        var dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setDriverClassName(driverClassName);
        dataSource.setMaximumPoolSize(1);
        return dataSource;
    }
}
