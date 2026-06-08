package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
public class BkmMockService {

    private final Random random = new Random();

    public BkmResponse processPaymentWithBkm(String transactionUuid, Double amount) {
        log.info("BKM MOCK: Received Payment Request for transaction UUID: {}, Amount: {}", transactionUuid, amount);
        
        try {
            // Simulate 1.5 seconds network / API processing delay
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("BKM MOCK: Simulation thread was interrupted.", e);
            return new BkmResponse(false, null, "Request interrupted");
        }

        // Simulate 80% success and 20% failure
        int chance = random.nextInt(100);
        if (chance < 80) {
            String approvalCode = "BKM-APP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            log.info("BKM MOCK: APPROVED for transaction {}. Approval Code: {}", transactionUuid, approvalCode);
            return new BkmResponse(true, approvalCode, "Success");
        } else {
            log.warn("BKM MOCK: REJECTED for transaction {}. Reason: Insufficient funds or fraud detection.", transactionUuid);
            return new BkmResponse(false, null, "Declined by BKM");
        }
    }

    // Response record representing response structure
    public record BkmResponse(boolean isSuccess, String approvalCode, String message) {}
}
