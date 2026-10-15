package com.lk.futurestracker.service;

import com.lk.futurestracker.model.Trade;
import com.lk.futurestracker.model.TradeSide;
import com.lk.futurestracker.model.TradeStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PnlServiceTest {

    private final PnlService pnlService = new PnlService();

    private Trade.TradeBuilder baseTrade() {
        return Trade.builder()
                .symbol("ES")
                .quantity(1)
                .pointValue(new BigDecimal("50"))
                .fees(BigDecimal.ZERO)
                .status(TradeStatus.CLOSED)
                .entryTime(LocalDateTime.of(2026, 10, 1, 9, 30))
                .exitTime(LocalDateTime.of(2026, 10, 1, 10, 0));
    }

    @Test
    void longTrade_profitsWhenExitAboveEntry() {
        Trade trade = baseTrade()
                .side(TradeSide.LONG)
                .entryPrice(new BigDecimal("5000"))
                .exitPrice(new BigDecimal("5010"))
                .build();

        // 10 points * $50/point * 1 contract = $500
        assertEquals(new BigDecimal("500.00"), pnlService.calculateRealizedPnl(trade));
    }

    @Test
    void longTrade_losesWhenExitBelowEntry() {
        Trade trade = baseTrade()
                .side(TradeSide.LONG)
                .entryPrice(new BigDecimal("5000"))
                .exitPrice(new BigDecimal("4990"))
                .build();

        assertEquals(new BigDecimal("-500.00"), pnlService.calculateRealizedPnl(trade));
    }

    @Test
    void shortTrade_profitsWhenExitBelowEntry() {
        Trade trade = baseTrade()
                .side(TradeSide.SHORT)
                .entryPrice(new BigDecimal("5000"))
                .exitPrice(new BigDecimal("4990"))
                .build();

        // Short profits as price falls: (5000-4990) effectively => +$500
        assertEquals(new BigDecimal("500.00"), pnlService.calculateRealizedPnl(trade));
    }

    @Test
    void shortTrade_losesWhenExitAboveEntry() {
        Trade trade = baseTrade()
                .side(TradeSide.SHORT)
                .entryPrice(new BigDecimal("5000"))
                .exitPrice(new BigDecimal("5010"))
                .build();

        assertEquals(new BigDecimal("-500.00"), pnlService.calculateRealizedPnl(trade));
    }

    @Test
    void feesAreAlwaysSubtracted_regardlessOfDirection() {
        Trade trade = baseTrade()
                .side(TradeSide.LONG)
                .entryPrice(new BigDecimal("5000"))
                .exitPrice(new BigDecimal("5010"))
                .fees(new BigDecimal("4.50"))
                .build();

        assertEquals(new BigDecimal("495.50"), pnlService.calculateRealizedPnl(trade));
    }

    @Test
    void multipleContracts_scalesLinearly() {
        Trade trade = baseTrade()
                .side(TradeSide.LONG)
                .quantity(3)
                .entryPrice(new BigDecimal("5000"))
                .exitPrice(new BigDecimal("5010"))
                .build();

        // 10 points * $50 * 3 contracts = $1500
        assertEquals(new BigDecimal("1500.00"), pnlService.calculateRealizedPnl(trade));
    }

    @Test
    void calculateRealizedPnl_throwsWhenNoExitPrice() {
        Trade openTrade = baseTrade()
                .side(TradeSide.LONG)
                .entryPrice(new BigDecimal("5000"))
                .exitPrice(null)
                .status(TradeStatus.OPEN)
                .build();

        assertThrows(IllegalStateException.class, () -> pnlService.calculateRealizedPnl(openTrade));
    }

    @Test
    void sumRealizedPnl_addsAcrossTrades_andIgnoresNulls() {
        Trade t1 = baseTrade().side(TradeSide.LONG).entryPrice(new BigDecimal("5000"))
                .exitPrice(new BigDecimal("5010")).build();
        t1.setRealizedPnl(pnlService.calculateRealizedPnl(t1));

        Trade t2 = baseTrade().side(TradeSide.SHORT).entryPrice(new BigDecimal("5000"))
                .exitPrice(new BigDecimal("5020")).build();
        t2.setRealizedPnl(pnlService.calculateRealizedPnl(t2)); // -1000

        assertEquals(new BigDecimal("-500.00"), pnlService.sumRealizedPnl(List.of(t1, t2)));
    }

    @Test
    void aggregateByExitDate_groupsAndSumsPerDay() {
        Trade day1Trade1 = baseTrade().side(TradeSide.LONG).entryPrice(new BigDecimal("5000"))
                .exitPrice(new BigDecimal("5010"))
                .exitTime(LocalDateTime.of(2026, 10, 1, 10, 0)).build();
        day1Trade1.setRealizedPnl(pnlService.calculateRealizedPnl(day1Trade1)); // +500

        Trade day1Trade2 = baseTrade().side(TradeSide.LONG).entryPrice(new BigDecimal("5000"))
                .exitPrice(new BigDecimal("5004"))
                .exitTime(LocalDateTime.of(2026, 10, 1, 14, 0)).build();
        day1Trade2.setRealizedPnl(pnlService.calculateRealizedPnl(day1Trade2)); // +200

        Trade day2Trade1 = baseTrade().side(TradeSide.SHORT).entryPrice(new BigDecimal("5000"))
                .exitPrice(new BigDecimal("5020"))
                .exitTime(LocalDateTime.of(2026, 10, 2, 10, 0)).build();
        day2Trade1.setRealizedPnl(pnlService.calculateRealizedPnl(day2Trade1)); // -1000

        Map<java.time.LocalDate, PnlService.DailyAggregate> result =
                pnlService.aggregateByExitDate(List.of(day1Trade1, day1Trade2, day2Trade1));

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("700.00"), result.get(java.time.LocalDate.of(2026, 10, 1)).pnl());
        assertEquals(2, result.get(java.time.LocalDate.of(2026, 10, 1)).tradeCount());
        assertEquals(new BigDecimal("-1000.00"), result.get(java.time.LocalDate.of(2026, 10, 2)).pnl());
        assertEquals(1, result.get(java.time.LocalDate.of(2026, 10, 2)).tradeCount());
    }
}
