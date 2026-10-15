package com.lk.futurestracker.dto;

import com.lk.futurestracker.model.TradeSide;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Body for creating a trade manually. If exitPrice/exitTime are supplied the trade is
 * created already CLOSED with realized PnL computed immediately — handy for backfilling
 * history. Otherwise it's created OPEN and closed later via TradeCloseRequest.
 */
@Getter
@Setter
public class TradeRequest {

    @NotNull(message = "accountId is required")
    private Long accountId;

    @NotBlank(message = "symbol is required")
    private String symbol;

    @NotNull(message = "side is required (LONG or SHORT)")
    private TradeSide side;

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be positive")
    private Integer quantity;

    /** Optional — if omitted, resolved from ContractSpecService by symbol. */
    @Positive(message = "pointValue must be positive when provided")
    private BigDecimal pointValue;

    @NotNull(message = "entryPrice is required")
    private BigDecimal entryPrice;

    @NotNull(message = "entryTime is required")
    private LocalDateTime entryTime;

    /** Optional: supply both exitPrice and exitTime to create an already-closed trade. */
    private BigDecimal exitPrice;
    private LocalDateTime exitTime;

    @PositiveOrZero(message = "fees cannot be negative")
    private BigDecimal fees;

    @Size(max = 500, message = "notes must be 500 characters or fewer")
    private String notes;
}
