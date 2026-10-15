package com.lk.futurestracker.service;

import com.lk.futurestracker.model.Trade;
import com.lk.futurestracker.model.TradeSide;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * All PnL math lives here, in one place, so the formula is defined exactly once and every
 * endpoint (trade close, daily summary, calendar, account equity) reads from the same source
 * of truth instead of re-deriving it.
 *
 * Scope note: this computes REALIZED PnL only (closed trades). Mark-to-market / unrealized
 * PnL on open positions would need a live price feed, which is out of scope for this backend —
 * open trades simply don't contribute to any PnL total until they're closed.
 */
@Service
public class PnlService {

    private static final int SCALE = 2;

    /**
     * (exitPrice - entryPrice) * quantity * pointValue, sign-flipped for shorts, minus fees.
     * Fees are a cost regardless of direction, so they're always subtracted.
     */
    public BigDecimal calculateRealizedPnl(Trade trade) {
        if (trade.getExitPrice() == null) {
            throw new IllegalStateException(
                    "Cannot calculate realized PnL for trade " + trade.getId() + ": no exit price set");
        }

        BigDecimal priceDiff = trade.getExitPrice().subtract(trade.getEntryPrice());
        if (trade.getSide() == TradeSide.SHORT) {
            priceDiff = priceDiff.negate();
        }

        BigDecimal gross = priceDiff
                .multiply(BigDecimal.valueOf(trade.getQuantity()))
                .multiply(trade.getPointValue());

        BigDecimal fees = trade.getFees() == null ? BigDecimal.ZERO : trade.getFees();

        return gross.subtract(fees).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal sumRealizedPnl(List<Trade> closedTrades) {
        return closedTrades.stream()
                .map(Trade::getRealizedPnl)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Groups closed trades by exit date (local date, no time component) and sums PnL per day —
     * exactly the shape a calendar-heatmap UI needs: date -> {pnl, tradeCount}.
     * Returned as a TreeMap so days come out in calendar order.
     */
    public Map<LocalDate, DailyAggregate> aggregateByExitDate(List<Trade> closedTrades) {
        Map<LocalDate, List<Trade>> byDay = closedTrades.stream()
                .filter(t -> t.getExitTime() != null)
                .collect(Collectors.groupingBy(t -> t.getExitTime().toLocalDate()));

        Map<LocalDate, DailyAggregate> result = new TreeMap<>();
        byDay.forEach((day, trades) -> result.put(day, new DailyAggregate(
                sumRealizedPnl(trades),
                trades.size()
        )));
        return result;
    }

    /** Simple value holder: one calendar day's worth of trading PnL. */
    public record DailyAggregate(BigDecimal pnl, int tradeCount) {
    }
}
