package com.example.demo.repository;

import com.example.demo.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TransactionRepository {

    private final JdbcTemplate jdbcTemplate;

    public void save(Transaction transaction) {
        String sql = "INSERT INTO open_banking_transactions (transaction_uuid, status, amount, created_at) VALUES (?, ?, ?, ?)";
        try {
            jdbcTemplate.update(sql, 
                    transaction.getTransactionUuid(), 
                    transaction.getStatus(), 
                    transaction.getAmount(), 
                    Timestamp.valueOf(transaction.getCreatedAt()));
            log.info("Successfully saved transaction to DB. UUID: {}, Status: {}", 
                    transaction.getTransactionUuid(), transaction.getStatus());
        } catch (Exception e) {
            log.error("Failed to save transaction to DB for UUID: {}", transaction.getTransactionUuid(), e);
            throw new RuntimeException("Database insertion failed", e);
        }
    }

    public void updateStatus(String uuid, String status) {
        String sql = "UPDATE open_banking_transactions SET status = ? WHERE transaction_uuid = ?";
        try {
            int rowsAffected = jdbcTemplate.update(sql, status, uuid);
            if (rowsAffected > 0) {
                log.info("Successfully updated transaction in DB. UUID: {}, New Status: {}", uuid, status);
            } else {
                log.warn("No transaction found in DB with UUID: {} during status update.", uuid);
            }
        } catch (Exception e) {
            log.error("Failed to update transaction status in DB for UUID: {}", uuid, e);
            throw new RuntimeException("Database update failed", e);
        }
    }
}
