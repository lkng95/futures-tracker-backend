package com.lk.futurestracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AccountRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "startingBalance is required")
    private BigDecimal startingBalance;
}
