package com.lk.futurestracker.dto;

import com.lk.futurestracker.model.Trade;
import com.lk.futurestracker.model.TradeSide;
import com.lk.futurestracker.model.TradeStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TradeResponse {

    private final Long id;
    private final Long accountId;
    private final String symbol;
    private final TradeSide side;
    private final Integer quantity;
    private final BigDecimal pointValue;
    private final BigDecimal entryPrice;
    private final BigDecimal exitPrice;
    private final LocalDateTime entryTime;
    private final LocalDateTime exitTime;
    private final BigDecimal fees;
    private final TradeStatus status;
    private final BigDecimal realizedPnl;
    private final String notes;

    public static TradeResponse from(Trade t) {
        return new TradeResponse(
                t.getId(),
                t.getAccount().getId(),
                t.getSymbol(),
                t.getSide(),
                t.getQuantity(),
                t.getPointValue(),
                t.getEntryPrice(),
                t.getExitPrice(),
                t.getEntryTime(),
                t.getExitTime(),
                t.getFees(),
                t.getStatus(),
                t.getRealizedPnl(),
                t.getNotes()
        );
    }
}
