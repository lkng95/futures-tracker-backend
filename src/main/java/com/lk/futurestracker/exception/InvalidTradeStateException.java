package com.lk.futurestracker.exception;

/** Thrown for operations that don't make sense given a trade's current state, e.g. closing an already-closed trade. */
public class InvalidTradeStateException extends RuntimeException {
    public InvalidTradeStateException(String message) {
        super(message);
    }
}
