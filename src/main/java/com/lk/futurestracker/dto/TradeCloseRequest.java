package com.lk.futurestracker.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Body for closing an open trade. */
@Getter
@Setter
public class TradeCloseRequest {

    @NotNull(message = "exitPrice is required")
    private BigDecimal exitPrice;

    @NotNull(message = "exitTime is required")
    private LocalDateTime exitTime;

    /** Optional: additional fees to add on close (e.g. exit commission), defaults to 0. */
    @PositiveOrZero(message = "fees cannot be negative")
    private BigDecimal fees;
}
