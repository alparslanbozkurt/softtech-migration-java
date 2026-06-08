package com.example.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "Sender IBAN cannot be blank")
    @Pattern(regexp = "^TR[0-9]{2}[0-9]{5}[0-9]{1}[0-9]{16}$", message = "Invalid Turkish IBAN format for Sender")
    private String senderIban;

    @NotBlank(message = "Receiver IBAN cannot be blank")
    @Pattern(regexp = "^TR[0-9]{2}[0-9]{5}[0-9]{1}[0-9]{16}$", message = "Invalid Turkish IBAN format for Receiver")
    private String receiverIban;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private Double amount;

    @NotBlank(message = "Currency cannot be blank")
    @Pattern(regexp = "^(TRY|USD|EUR)$", message = "Supported currencies are TRY, USD, and EUR")
    private String currency;

    private String description;
}
