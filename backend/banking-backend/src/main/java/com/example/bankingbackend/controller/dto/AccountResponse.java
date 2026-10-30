package com.example.bankingbackend.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        String maskedAccountNumber,
        BigDecimal balance,
        String currency,
        String status,
        LocalDateTime createdAt
) {}
