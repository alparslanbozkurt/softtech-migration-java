package com.example.demo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job transactionReconciliationJob;

    @PostMapping("/run")
    public ResponseEntity<String> runReconciliationJob() {
        log.info("REST Request received to launch transactionReconciliationJob");
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();
            
            var execution = jobLauncher.run(transactionReconciliationJob, jobParameters);
            
            log.info("Batch job started successfully. Status: {}", execution.getStatus());
            return ResponseEntity.ok("Batch job started. Current Status: " + execution.getStatus());
        } catch (Exception e) {
            log.error("Failed to run Batch job", e);
            return ResponseEntity.internalServerError().body("Failed to run Batch job: " + e.getMessage());
        }
    }
}
