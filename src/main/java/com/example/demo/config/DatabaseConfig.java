package com.example.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Configuration
public class DatabaseConfig {

    @Bean
    public CommandLineRunner databaseInitializer(JdbcTemplate jdbcTemplate) {
        return args -> {
            log.info("Initializing database schema...");
            initializeMssqlSchema(jdbcTemplate);
        };
    }

    private void initializeMssqlSchema(JdbcTemplate jdbcTemplate) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[open_banking_transactions]') AND type in (N'U')", Integer.class);
            if (count == null || count == 0) {
                String createTableSql = "CREATE TABLE open_banking_transactions (" +
                    "id BIGINT IDENTITY(1,1) PRIMARY KEY, " +
                    "transaction_uuid VARCHAR(36) NOT NULL UNIQUE, " +
                    "status VARCHAR(20) NOT NULL, " +
                    "amount DECIMAL(15, 2) NOT NULL, " +
                    "created_at DATETIME2 NOT NULL" +
                    ")";
                jdbcTemplate.execute(createTableSql);
                log.info("MSSQL schema initialized successfully. Table 'open_banking_transactions' created.");
            } else {
                log.info("MSSQL schema already exists. Skipping creation.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize MSSQL schema: {}", e.getMessage());
        }
    }
}
