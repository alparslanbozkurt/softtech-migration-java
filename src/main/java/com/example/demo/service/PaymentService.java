package com.example.demo.service;

import com.example.demo.dto.PaymentEvent;
import com.example.demo.dto.PaymentRequest;
import com.example.demo.model.Transaction;
import com.example.demo.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    private static final String TOPIC = "payment-events";
    private static final long REDIS_TTL_MINUTES = 10;

    public String initiatePayment(PaymentRequest request) {
        // 1. Assign unique transaction number (UUID)
        String transactionUuid = UUID.randomUUID().toString();
        log.info("Initiating Payment. Generated Transaction UUID: {}", transactionUuid);

        // 2. Save to MySQL DB as "PENDING" via JdbcTemplate
        Transaction transaction = Transaction.builder()
                .transactionUuid(transactionUuid)
                .status("PENDING")
                .amount(request.getAmount())
                .createdAt(LocalDateTime.now())
                .build();
        
        transactionRepository.save(transaction);

        // 3. Save status to Redis (TTL 10 mins)
        try {
            redisTemplate.opsForValue().set(transactionUuid, "PENDING", REDIS_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("Payment status 'PENDING' cached in Redis. Key: {}", transactionUuid);
        } catch (Exception e) {
            log.error("Failed to write payment status to Redis for key: {}", transactionUuid, e);
        }

        // 4. Create PaymentEvent
        PaymentEvent event = PaymentEvent.builder()
                .transactionUuid(transactionUuid)
                .senderIban(request.getSenderIban())
                .receiverIban(request.getReceiverIban())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        // 5. Publish to Kafka 'payment-events' topic asynchronously
        kafkaTemplate.send(TOPIC, transactionUuid, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send Payment Event to Kafka for transaction UUID: {}", transactionUuid, ex);
                    } else {
                        log.info("Payment Event sent successfully to Kafka topic: {} for UUID: {}. Offset: {}", 
                                TOPIC, transactionUuid, result.getRecordMetadata().offset());
                    }
                });

        // 6. Return transaction UUID instantly
        return transactionUuid;
    }
}
