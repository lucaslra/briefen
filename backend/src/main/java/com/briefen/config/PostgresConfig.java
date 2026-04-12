package com.briefen.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "briefen.db.type", havingValue = "postgres")
@EnableJpaRepositories(basePackages = "com.briefen.persistence.jpa")
public class PostgresConfig {

    @Bean
    public DataSource dataSource(
            @Value("${BRIEFEN_DATASOURCE_URL}") String url,
            @Value("${BRIEFEN_DATASOURCE_USERNAME}") String username,
            @Value("${BRIEFEN_DATASOURCE_PASSWORD}") String password) {
        var dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(2);
        return dataSource;
    }
}
