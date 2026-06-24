package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class BatchSchedulerService {

    private final JobLauncher jobLauncher;
    private final Job transactionReconciliationJob;

    /**
     * Periodically runs the Transaction Reconciliation Batch Job.
     * The delay is read from properties, defaulting to 30 seconds.
     */
    @Scheduled(fixedDelayString = "${batchScheduler.fixedDelay.in.milliseconds:30000}")
    public void runReconciliationJob() {
        log.info("BatchScheduler - Triggering transactionReconciliationJob at {}", LocalDateTime.now());
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();
            
            var execution = jobLauncher.run(transactionReconciliationJob, jobParameters);
            log.info("BatchScheduler - Job executed successfully. Status: {}", execution.getStatus());
        } catch (Exception e) {
            log.error("BatchScheduler - Failed to run Batch job", e);
        }
    }
}
