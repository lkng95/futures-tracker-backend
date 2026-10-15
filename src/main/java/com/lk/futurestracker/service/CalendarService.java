package com.lk.futurestracker.service;

import com.lk.futurestracker.dto.CalendarDayResponse;
import com.lk.futurestracker.model.Trade;
import com.lk.futurestracker.model.TradeStatus;
import com.lk.futurestracker.repository.TradeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Produces a full day-by-day PnL calendar for one month, with every day present (zero-filled)
 * so a calendar-grid UI can render the whole month without having to patch in gaps itself.
 */
@Service
public class CalendarService {

    private final TradeRepository tradeRepository;
    private final PnlService pnlService;

    public CalendarService(TradeRepository tradeRepository, PnlService pnlService) {
        this.tradeRepository = tradeRepository;
        this.pnlService = pnlService;
    }

    public List<CalendarDayResponse> getMonth(Long accountId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime from = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime to = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        List<Trade> closedTradesInMonth = tradeRepository
                .findByAccountIdAndStatusAndExitTimeBetween(accountId, TradeStatus.CLOSED, from, to);

        Map<LocalDate, PnlService.DailyAggregate> byDay = pnlService.aggregateByExitDate(closedTradesInMonth);

        List<CalendarDayResponse> days = new ArrayList<>();
        for (int d = 1; d <= yearMonth.lengthOfMonth(); d++) {
            LocalDate date = yearMonth.atDay(d);
            PnlService.DailyAggregate agg = byDay.get(date);
            days.add(new CalendarDayResponse(
                    date,
                    agg == null ? BigDecimal.ZERO.setScale(2) : agg.pnl(),
                    agg == null ? 0 : agg.tradeCount()
            ));
        }
        return days;
    }
}
