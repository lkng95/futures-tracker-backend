package com.lk.futurestracker.controller;

import com.lk.futurestracker.dto.AccountRequest;
import com.lk.futurestracker.dto.AccountResponse;
import com.lk.futurestracker.dto.AccountSummaryResponse;
import com.lk.futurestracker.model.Account;
import com.lk.futurestracker.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody AccountRequest request) {
        Account account = accountService.createAccount(request);
        return AccountResponse.from(account);
    }

    @GetMapping
    public List<AccountResponse> list() {
        return accountService.listAccounts().stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AccountResponse getById(@PathVariable Long id) {
        return AccountResponse.from(accountService.getAccountOrThrow(id));
    }

    /** The headline "account size" view: starting balance, realized PnL, today's PnL, current equity. */
    @GetMapping("/{id}/summary")
    public AccountSummaryResponse summary(@PathVariable Long id) {
        return accountService.getSummary(id);
    }
}
