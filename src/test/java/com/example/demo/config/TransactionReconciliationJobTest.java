package com.example.demo.config;

import com.example.demo.model.Transaction;
import com.example.demo.repository.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "batch.scheduler.enabled=false")
@SpringBatchTest
public class TransactionReconciliationJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        // Clean table before testing
        jdbcTemplate.execute("DELETE FROM open_banking_transactions");
        
        // Remove old CSV file if exists
        File csvFile = new File("reconciled_transactions.csv");
        if (csvFile.exists()) {
            csvFile.delete();
        }
    }

    @AfterEach
    public void tearDown() {
        // Clean up CSV file after test run
        File csvFile = new File("reconciled_transactions.csv");
        if (csvFile.exists()) {
            csvFile.delete();
        }
    }

    @Test
    public void testReconciliationJobSuccess() throws Exception {
        // 1. Insert test transactions
        Transaction t1 = Transaction.builder()
                .transactionUuid("uuid-approved")
                .status("APPROVED")
                .amount(150.50)
                .createdAt(LocalDateTime.now())
                .build();

        Transaction t2 = Transaction.builder()
                .transactionUuid("uuid-rejected")
                .status("REJECTED")
                .amount(75.00)
                .createdAt(LocalDateTime.now())
                .build();

        Transaction t3 = Transaction.builder()
                .transactionUuid("uuid-pending")
                .status("PENDING")
                .amount(300.00)
                .createdAt(LocalDateTime.now())
                .build();

        transactionRepository.save(t1);
        transactionRepository.save(t2);
        transactionRepository.save(t3);

        // 2. Launch the Spring Batch job via JobLauncherTestUtils
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // 3. Verify Job completed successfully
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        // 4. Verify DB updates (APPROVED & REJECTED -> RECONCILED, PENDING should stay PENDING)
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT transaction_uuid, status FROM open_banking_transactions");

        assertThat(results).hasSize(3);
        
        String status1 = getStatusByUuid(results, "uuid-approved");
        String status2 = getStatusByUuid(results, "uuid-rejected");
        String status3 = getStatusByUuid(results, "uuid-pending");

        assertThat(status1).isEqualTo("RECONCILED");
        assertThat(status2).isEqualTo("RECONCILED");
        assertThat(status3).isEqualTo("PENDING");

        // 5. Verify CSV output file creation
        File csvFile = new File("reconciled_transactions.csv");
        assertThat(csvFile.exists()).isTrue();
    }

    private String getStatusByUuid(List<Map<String, Object>> results, String uuid) {
        return results.stream()
                .filter(m -> uuid.equals(m.get("transaction_uuid")))
                .map(m -> (String) m.get("status"))
                .findFirst()
                .orElse(null);
    }
}
