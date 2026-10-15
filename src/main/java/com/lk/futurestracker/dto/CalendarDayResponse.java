package com.lk.futurestracker.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class CalendarDayResponse {
    private final LocalDate date;
    private final BigDecimal pnl;
    private final int tradeCount;
}
