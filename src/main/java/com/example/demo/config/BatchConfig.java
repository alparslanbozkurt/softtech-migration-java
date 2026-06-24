package com.example.demo.config;

import com.example.demo.model.Transaction;
import com.example.demo.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class BatchConfig {

    @Bean
    public JdbcCursorItemReader<Transaction> transactionReader(DataSource dataSource) {
        log.info("Initializing Batch Reader for transactions...");
        return new JdbcCursorItemReaderBuilder<Transaction>()
                .name("transactionReader")
                .dataSource(dataSource)
                .sql("SELECT id, transaction_uuid, status, amount, created_at FROM open_banking_transactions WHERE status IN ('APPROVED', 'REJECTED')")
                .rowMapper((rs, rowNum) -> Transaction.builder()
                        .id(rs.getLong("id"))
                        .transactionUuid(rs.getString("transaction_uuid"))
                        .status(rs.getString("status"))
                        .amount(rs.getDouble("amount"))
                        .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                        .build())
                .build();
    }

    @Bean
    public ItemProcessor<Transaction, Transaction> transactionProcessor() {
        return transaction -> {
            log.info("Batch Processing - Reconciling transaction: {}", transaction.getTransactionUuid());
            transaction.setStatus("RECONCILED");
            return transaction;
        };
    }

    @Bean
    public FlatFileItemWriter<Transaction> csvWriter() {
        log.info("Initializing CSV Writer for reconciled transactions...");
        return new FlatFileItemWriterBuilder<Transaction>()
                .name("csvWriter")
                .resource(new FileSystemResource("reconciled_transactions.csv"))
                .delimited()
                .delimiter(",")
                .names("id", "transactionUuid", "status", "amount", "createdAt")
                .headerCallback(writer -> writer.write("ID,TransactionUUID,Status,Amount,CreatedAt"))
                .build();
    }

    @Bean
    public Step reconcileStep(JobRepository jobRepository, 
                             PlatformTransactionManager transactionManager,
                             JdbcCursorItemReader<Transaction> reader,
                             ItemProcessor<Transaction, Transaction> processor,
                             FlatFileItemWriter<Transaction> csvWriter,
                             TransactionRepository transactionRepository) {
        
        ItemWriter<Transaction> customWriter = chunk -> {
            log.info("Batch Writing - Saving chunk of size {} to CSV and updating DB", chunk.size());
            // 1. Write chunk to CSV
            csvWriter.write(chunk);
            
            // 2. Update status in database to RECONCILED
            for (Transaction transaction : chunk) {
                transactionRepository.updateStatus(transaction.getTransactionUuid(), "RECONCILED");
            }
        };

        return new StepBuilder("reconcileStep", jobRepository)
                .<Transaction, Transaction>chunk(5, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(customWriter)
                .stream(csvWriter) // Registers the CSV writer to manage its open/close lifecycle
                .build();
    }

    @Bean
    public Job transactionReconciliationJob(JobRepository jobRepository, Step reconcileStep) {
        log.info("Initializing Batch Job: transactionReconciliationJob");
        return new JobBuilder("transactionReconciliationJob", jobRepository)
                .start(reconcileStep)
                .build();
    }
}
