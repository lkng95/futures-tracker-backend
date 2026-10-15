package com.lk.futurestracker.repository;

import com.lk.futurestracker.model.Trade;
import com.lk.futurestracker.model.TradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findByAccountIdOrderByEntryTimeDesc(Long accountId);

    List<Trade> findByAccountIdAndStatus(Long accountId, TradeStatus status);

    /** Closed trades whose exit falls within [from, to) — the basis for PnL/calendar aggregation. */
    List<Trade> findByAccountIdAndStatusAndExitTimeBetween(
            Long accountId, TradeStatus status, LocalDateTime from, LocalDateTime to);
}
