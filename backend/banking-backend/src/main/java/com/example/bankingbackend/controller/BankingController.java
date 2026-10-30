package com.example.bankingbackend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bankingbackend.controller.dto.MoneyRequest;
import com.example.bankingbackend.controller.dto.TransferRequest;
import com.example.bankingbackend.service.BankingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/banking")
public class BankingController {

    private final BankingService bankingService;

    public BankingController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @PostMapping("/accounts/{accountId}/deposit")
    public String deposit(
            @PathVariable Long accountId,
            @Valid @RequestBody MoneyRequest request,
            @AuthenticationPrincipal UserDetails user) {

        bankingService.deposit(
                accountId, user.getUsername(), request.amount());

        return "Deposit completed";
    }

    @PostMapping("/accounts/{accountId}/withdraw")
    public String withdraw(
            @PathVariable Long accountId,
            @Valid @RequestBody MoneyRequest request,
            @AuthenticationPrincipal UserDetails user) {

        bankingService.withdraw(
                accountId, user.getUsername(), request.amount());

        return "Withdrawal completed";
    }

    @PostMapping("/transfer")
    public String transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal UserDetails user) {

        bankingService.transfer(
                request.fromAccountId(),
                request.toAccountId(),
                user.getUsername(),
                request.amount());

        return "Transfer completed";
    }
}
