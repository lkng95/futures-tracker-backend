package com.lk.futurestracker.controller;

import com.lk.futurestracker.dto.CalendarDayResponse;
import com.lk.futurestracker.service.CalendarService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    /** e.g. GET /api/calendar?accountId=1&year=2026&month=10 */
    @GetMapping
    public List<CalendarDayResponse> getMonth(@RequestParam Long accountId,
                                               @RequestParam int year,
                                               @RequestParam int month) {
        return calendarService.getMonth(accountId, year, month);
    }
}
