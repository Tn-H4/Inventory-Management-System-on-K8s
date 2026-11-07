package org.pdm.backend.security;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.SQLException;

@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Bean
    public HikariDataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        HikariDataSource dataSource = new HikariDataSource(config);
        // Store reference for static helper methods
        DataSourceHolder.dataSource = dataSource;
        return dataSource;
    }

    /**
     * Static helper for backward compatibility with RepositoryImpl classes.
     * Delegates to the Spring-managed DataSource bean.
     * @return Connection object that represents a live link to the database.
     * @throws SQLException in case of connection failure.
     * @throws IllegalStateException if DataSource not initialized (called before Spring context ready).
     */
    public static Connection getConnection() throws SQLException {
        HikariDataSource ds = getDataSource();
        if (ds == null) {
            throw new IllegalStateException("DataSource not initialized. Called before Spring context ready.");
        }
        return ds.getConnection();
    }

    /**
     * Static helper for backward compatibility.
     * @return the HikariDataSource bean, or null if not yet initialized.
     */
    public static HikariDataSource getDataSource() {
        return DataSourceHolder.getDataSource();
    }

    /**
     * Holder class to allow static access to the Spring-managed DataSource.
     * This is populated by Spring after the @Bean method is invoked.
     */
    private static class DataSourceHolder {
        private static HikariDataSource dataSource;

        private static HikariDataSource getDataSource() {
            return dataSource;
        }
    }
}