package com.example.bankingbackend.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankingbackend.entity.Account;
import com.example.bankingbackend.entity.Transaction;
import com.example.bankingbackend.entity.TransactionStatus;
import com.example.bankingbackend.entity.TransactionType;
import com.example.bankingbackend.repository.AccountRepository;
import com.example.bankingbackend.repository.TransactionRepository;


@Service
public class BankingService {

     private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;


    public BankingService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public void deposit(Long accountId, String email, BigDecimal amount) {
        validateAmount(amount);

        Account account = getOwnedAccount(accountId, email);
        account.setBalance(account.getBalance().add(amount));

        saveTransaction(account, null, amount, TransactionType.DEPOSIT);
    }

        @Transactional
    public void withdraw(Long accountId, String email, BigDecimal amount) {
        validateAmount(amount);

        Account account = getOwnedAccount(accountId, email);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(amount));

        saveTransaction(account, null, amount, TransactionType.WITHDRAWAL);
    }

       @Transactional
    public void transfer(
            Long fromAccountId,
            Long toAccountId,
            String email,
            BigDecimal amount) {

        validateAmount(amount);

        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException(
                    "Source and destination accounts must differ");
        }

        // Lock in a consistent ID order to reduce deadlock risk.
        Long firstId = Math.min(fromAccountId, toAccountId);
        Long secondId = Math.max(fromAccountId, toAccountId);

        Account first = getOwnedAccount(firstId, email);
        Account second = getOwnedAccount(secondId, email);

        Account from = fromAccountId.equals(firstId) ? first : second;
        Account to = toAccountId.equals(firstId) ? first : second;

        if (from.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        saveTransaction(from, to, amount, TransactionType.TRANSFER);
    }

    private Account getOwnedAccount(Long accountId, String email) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found"));

        if (!account.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("Account not found");
        }

        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }

        return account;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0
                || amount.scale() > 2
                || amount.precision() > 19) {
            throw new IllegalArgumentException("Invalid amount");
        }
    }

    private void saveTransaction(
            Account from,
            Account to,
            BigDecimal amount,
            TransactionType type) {

        Transaction transaction = new Transaction();
        transaction.setFromAccount(from);
        transaction.setToAccount(to);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setStatus(TransactionStatus.COMPLETED);

        transactionRepository.save(transaction);
    }
    
}
