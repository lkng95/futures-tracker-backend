package com.lk.futurestracker.model;

/** Lifecycle state of a trade. OPEN trades have no exit yet and no realized PnL. */
public enum TradeStatus {
    OPEN,
    CLOSED
}
