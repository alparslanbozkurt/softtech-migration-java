package com.example.demo.consumer;

import com.example.demo.dto.PaymentEvent;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.service.BkmMockService;
import com.example.demo.service.BkmMockService.BkmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConsumer {

    private final BkmMockService bkmMockService;
    private final TransactionRepository transactionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TOPIC = "payment-events";
    private static final long REDIS_TTL_MINUTES = 10;

    @KafkaListener(topics = TOPIC, groupId = "open-banking-group")
    public void consumePaymentEvent(PaymentEvent event) {
        log.info("=========================================================");
        log.info("KAFKA CONSUMER: Received payment event from topic: {}", TOPIC);
        log.info("Transaction UUID: {}", event.getTransactionUuid());
        log.info("Sender IBAN     : {}", event.getSenderIban());
        log.info("Receiver IBAN   : {}", event.getReceiverIban());
        log.info("Amount          : {} {}", event.getAmount(), event.getCurrency());
        log.info("Current Status  : {}", event.getStatus());
        log.info("=========================================================");

        // 1. Call BkmMockService (Simulates 1.5s delay and 80% success / 20% failure)
        log.info("Initiating verification with BKM for Transaction: {}", event.getTransactionUuid());
        BkmResponse bkmResponse = bkmMockService.processPaymentWithBkm(event.getTransactionUuid(), event.getAmount());

        // 2. Map response status
        String finalStatus = bkmResponse.isSuccess() ? "APPROVED" : "REJECTED";
        log.info("BKM response received. Updating status to {} for Transaction: {}", finalStatus, event.getTransactionUuid());

        // 3. Update database record via JdbcTemplate repository
        try {
            transactionRepository.updateStatus(event.getTransactionUuid(), finalStatus);
        } catch (Exception e) {
            log.error("Failed to update status in DB to {} for Transaction: {}", finalStatus, event.getTransactionUuid(), e);
        }

        // 4. Update status in Redis cache
        try {
            redisTemplate.opsForValue().set(event.getTransactionUuid(), finalStatus, REDIS_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("Redis cache updated successfully. Key: {}, New Status: {}", event.getTransactionUuid(), finalStatus);
        } catch (Exception e) {
            log.error("Failed to update status in Redis to {} for Transaction: {}", finalStatus, event.getTransactionUuid(), e);
        }
    }
}
