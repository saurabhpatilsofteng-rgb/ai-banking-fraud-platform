package com.example.bankingbackend.controller;




import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.bankingbackend.controller.dto.AccountResponse;
import com.example.bankingbackend.controller.dto.CreateAccountResponse;
import com.example.bankingbackend.service.AccountService;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    public List<AccountResponse> getMyAccounts(
            @AuthenticationPrincipal UserDetails user) {
        return accountService.getMyAccounts(user.getUsername());
    }

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateAccountResponse createMyAccount(
            @AuthenticationPrincipal UserDetails user) {
        return accountService.createMyAccount(user.getUsername());
    }
}
