package com.example.bankingbackend.controller.dto;

public record FraudPredictionResponse(
        double risk_score,
        String risk_level,
        boolean review_recommended,
        String model_type
) {}