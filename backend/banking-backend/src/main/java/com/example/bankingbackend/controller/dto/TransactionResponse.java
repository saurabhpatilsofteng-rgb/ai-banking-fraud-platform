package com.example.bankingbackend.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.bankingbackend.entity.TransactionStatus;
import com.example.bankingbackend.entity.TransactionType;

public record TransactionResponse(
        Long id,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        TransactionType type,
        TransactionStatus status,
        LocalDateTime createdAt
) {}
