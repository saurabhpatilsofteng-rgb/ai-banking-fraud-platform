package com.example.bankingbackend.auth.dto;

public record AuthResponse(
        String accessToken,
        String tokenType) {
}
