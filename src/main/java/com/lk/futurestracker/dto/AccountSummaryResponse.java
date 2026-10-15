package com.lk.futurestracker.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Account size/equity snapshot. currentEquity and totalRealizedPnl are always derived from
 * the trade history at request time — never stored — so they can't drift out of sync.
 */
@Getter
@AllArgsConstructor
public class AccountSummaryResponse {
    private final Long accountId;
    private final String name;
    private final BigDecimal startingBalance;
    private final BigDecimal totalRealizedPnl;
    private final BigDecimal todayPnl;
    private final BigDecimal currentEquity;
    private final long openTradeCount;
    private final long closedTradeCount;
}
