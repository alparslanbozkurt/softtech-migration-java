package com.example.demo.controller;

import com.example.demo.dto.PaymentRequest;
import com.example.demo.dto.PaymentResponse;
import com.example.demo.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> initiatePayment(@Valid @RequestBody PaymentRequest paymentRequest) {
        log.info("Received Payment Initiation request from sender: {}", paymentRequest.getSenderIban());
        
        String transactionUuid = paymentService.initiatePayment(paymentRequest);
        
        PaymentResponse response = PaymentResponse.builder()
                .transactionUuid(transactionUuid)
                .status("PENDING")
                .message("Payment request has been accepted and is being processed.")
                .timestamp(LocalDateTime.now().toString())
                .build();
                
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
