package com.example.bankingbackend.service;


import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankingbackend.controller.dto.AccountResponse;
import com.example.bankingbackend.controller.dto.CreateAccountResponse;
import com.example.bankingbackend.entity.Account;
import com.example.bankingbackend.entity.User;
import com.example.bankingbackend.repository.AccountRepository;
import com.example.bankingbackend.repository.UserRepository;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountService(
            AccountRepository accountRepository,
            UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getMyAccounts(String email) {
        return accountRepository.findByUser_Email(email)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CreateAccountResponse createMyAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        Account account = new Account();
        account.setUser(user);
        account.setAccountNumber(generateAccountNumber());
        account.setBalance(BigDecimal.ZERO);
        account.setCurrency("INR");
        account.setStatus("ACTIVE");

        Account saved = accountRepository.save(account);

        return new CreateAccountResponse(
                saved.getId(),
                maskAccountNumber(saved.getAccountNumber()),
                "Account created with zero balance"
        );
    }

    private String generateAccountNumber() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String number = "10" + String.format(
                    "%010d", secureRandom.nextInt(1_000_000_000));

            if (!accountRepository.existsByAccountNumber(number)) {
                return number;
            }
        }

        throw new IllegalStateException(
                "Could not generate a unique account number");
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                maskAccountNumber(account.getAccountNumber()),
                account.getBalance(),
                account.getCurrency(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }

    private String maskAccountNumber(String number) {
        if (number == null || number.length() <= 4) {
            return "****";
        }

        return "*".repeat(number.length() - 4)
                + number.substring(number.length() - 4);
    }
}