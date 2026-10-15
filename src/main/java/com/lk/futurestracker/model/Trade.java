package com.lk.futurestracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A single futures trade (one round-turn, or an open position waiting to be closed).
 *
 * pointValue is captured at creation time from ContractSpecService rather than looked
 * up live on every read — so if the reference table for a symbol ever changes, past
 * trades keep the point value that was actually in effect when they were recorded.
 */
@Entity
@Table(name = "trades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TradeSide side;

    @Column(nullable = false)
    private Integer quantity;

    /** Dollar value of one full point of price movement, per contract. */
    @Column(name = "point_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal pointValue;

    @Column(name = "entry_price", nullable = false, precision = 19, scale = 6)
    private BigDecimal entryPrice;

    @Column(name = "exit_price", precision = 19, scale = 6)
    private BigDecimal exitPrice;

    @Column(name = "entry_time", nullable = false)
    private LocalDateTime entryTime;

    @Column(name = "exit_time")
    private LocalDateTime exitTime;

    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal fees = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TradeStatus status;

    /** Null while OPEN; set by PnlService the moment the trade is closed. */
    @Column(name = "realized_pnl", precision = 19, scale = 4)
    private BigDecimal realizedPnl;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
