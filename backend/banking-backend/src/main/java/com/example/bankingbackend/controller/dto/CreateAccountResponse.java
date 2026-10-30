package com.example.bankingbackend.controller.dto;

public record CreateAccountResponse(
        Long id,
        String maskedAccountNumber,
        String message
) {}