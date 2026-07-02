package com.example.demo.repository;

import com.example.demo.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Slf4j
@Repository
public class TransactionRepository {

    private final JdbcTemplate mssqlJdbcTemplate;
    private final JdbcTemplate oracleJdbcTemplate;

    public TransactionRepository(
            @Qualifier("mssqlJdbcTemplate") JdbcTemplate mssqlJdbcTemplate,
            @Qualifier("oracleJdbcTemplate") JdbcTemplate oracleJdbcTemplate) {
        this.mssqlJdbcTemplate = mssqlJdbcTemplate;
        this.oracleJdbcTemplate = oracleJdbcTemplate;
    }

    /**
     * Her iki veritabanına da (MSSQL + Oracle) aynı anda kayıt yapar (Dual Write).
     */
    public void save(Transaction transaction) {
        saveTo(mssqlJdbcTemplate, "MSSQL", transaction);
        saveTo(oracleJdbcTemplate, "Oracle", transaction);
    }

    /**
     * Her iki veritabanındaki kaydın statüsünü günceller (Dual Update).
     */
    public void updateStatus(String uuid, String status) {
        updateStatusOn(mssqlJdbcTemplate, "MSSQL", uuid, status);
        updateStatusOn(oracleJdbcTemplate, "Oracle", uuid, status);
    }

    // ==========================================
    // Private Helpers
    // ==========================================

    private void saveTo(JdbcTemplate jdbcTemplate, String dbName, Transaction transaction) {
        String sql = "INSERT INTO open_banking_transactions (transaction_uuid, status, amount, created_at) VALUES (?, ?, ?, ?)";

        try {
            jdbcTemplate.update(sql,
                    transaction.getTransactionUuid(),
                    transaction.getStatus(),
                    transaction.getAmount(),
                    Timestamp.valueOf(transaction.getCreatedAt()));
            log.info("[{}] Successfully saved transaction. UUID: {}, Status: {}",
                    dbName, transaction.getTransactionUuid(), transaction.getStatus());
        } catch (Exception e) {
            log.error("[{}] Failed to save transaction for UUID: {}", dbName, transaction.getTransactionUuid(), e);
        }
    }

    private void updateStatusOn(JdbcTemplate jdbcTemplate, String dbName, String uuid, String status) {
        String sql = "UPDATE open_banking_transactions SET status = ? WHERE transaction_uuid = ?";

        try {
            int rowsAffected = jdbcTemplate.update(sql, status, uuid);
            if (rowsAffected > 0) {
                log.info("[{}] Successfully updated transaction. UUID: {}, New Status: {}", dbName, uuid, status);
            } else {
                log.warn("[{}] No transaction found with UUID: {} during status update.", dbName, uuid);
            }
        } catch (Exception e) {
            log.error("[{}] Failed to update transaction status for UUID: {}", dbName, uuid, e);
        }
    }
}
