package com.example.bankingbackend.controller.dto;

import java.time.LocalDateTime;

public record FraudAssessmentResponse(
        Long assessmentId,
        Long transactionId,
        double riskScore,
        String riskLevel,
        boolean reviewRecommended,
        String modelType,
        LocalDateTime assessedAt
) {}
