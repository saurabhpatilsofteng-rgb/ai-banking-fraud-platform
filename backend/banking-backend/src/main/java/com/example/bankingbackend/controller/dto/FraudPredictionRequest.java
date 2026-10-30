package com.example.bankingbackend.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FraudPredictionRequest(
        double amount,
        int hour,

        @JsonProperty("transactions_last_hour")
        int transactionsLastHour,

        @JsonProperty("new_device")
        int newDevice,

        @JsonProperty("international")
        int international
) {}