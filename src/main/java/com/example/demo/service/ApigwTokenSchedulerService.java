package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class ApigwTokenSchedulerService {

    /**
     * Periodically updates the API Gateway token.
     * The delay interval is read dynamically from application properties/yml.
     */
    @Scheduled(fixedDelayString = "${fixedDelayForApigwToken.in.milliseconds}")
    public void updateApigwToken() {
        log.info("updateApigwToken() scheduled task executed successfully at: {}", LocalDateTime.now());
    }
}
