package com.lk.futurestracker.dto;

import com.lk.futurestracker.model.Account;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AccountResponse {
    private final Long id;
    private final String name;
    private final BigDecimal startingBalance;
    private final LocalDateTime createdAt;

    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getId(), a.getName(), a.getStartingBalance(), a.getCreatedAt());
    }
}
