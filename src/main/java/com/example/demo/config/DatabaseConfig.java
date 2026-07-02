package com.example.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class DatabaseConfig {

    // ==========================================
    // MSSQL DataSource & JdbcTemplate (Primary)
    // ==========================================

    @Primary
    @Bean(name = "mssqlDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.mssql")
    public DataSource mssqlDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean(name = "mssqlJdbcTemplate")
    public JdbcTemplate mssqlJdbcTemplate(@Qualifier("mssqlDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // ==========================================
    // Oracle DataSource & JdbcTemplate
    // ==========================================

    @Bean(name = "oracleDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.oracle")
    public DataSource oracleDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "oracleJdbcTemplate")
    public JdbcTemplate oracleJdbcTemplate(@Qualifier("oracleDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // ==========================================
    // Schema Initialization
    // ==========================================

    @Bean
    public CommandLineRunner databaseInitializer(
            @Qualifier("mssqlJdbcTemplate") JdbcTemplate mssqlJdbcTemplate,
            @Qualifier("oracleJdbcTemplate") JdbcTemplate oracleJdbcTemplate) {
        return args -> {
            log.info("Initializing database schemas...");
            initializeMssqlSchema(mssqlJdbcTemplate);
            initializeOracleSchema(oracleJdbcTemplate);
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

    private void initializeOracleSchema(JdbcTemplate jdbcTemplate) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM user_tables WHERE table_name = 'OPEN_BANKING_TRANSACTIONS'", Integer.class);
            if (count == null || count == 0) {
                String createTableSql = "CREATE TABLE open_banking_transactions (" +
                    "id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                    "transaction_uuid VARCHAR2(36) NOT NULL UNIQUE, " +
                    "status VARCHAR2(20) NOT NULL, " +
                    "amount NUMBER(15, 2) NOT NULL, " +
                    "created_at TIMESTAMP NOT NULL" +
                    ")";
                jdbcTemplate.execute(createTableSql);
                log.info("Oracle schema initialized successfully. Table 'OPEN_BANKING_TRANSACTIONS' created.");
            } else {
                log.info("Oracle schema already exists. Skipping creation.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Oracle schema: {}", e.getMessage());
        }
    }
}
