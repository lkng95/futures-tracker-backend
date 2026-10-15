package com.lk.futurestracker.controller;

import com.lk.futurestracker.dto.TradeCloseRequest;
import com.lk.futurestracker.dto.TradeRequest;
import com.lk.futurestracker.dto.TradeResponse;
import com.lk.futurestracker.model.Trade;
import com.lk.futurestracker.model.TradeStatus;
import com.lk.futurestracker.service.TradeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TradeResponse create(@Valid @RequestBody TradeRequest request) {
        Trade trade = tradeService.createTrade(request);
        return TradeResponse.from(trade);
    }

    @GetMapping("/{id}")
    public TradeResponse getById(@PathVariable Long id) {
        return TradeResponse.from(tradeService.getTradeOrThrow(id));
    }

    @GetMapping
    public List<TradeResponse> list(@RequestParam Long accountId,
                                     @RequestParam(required = false) TradeStatus status) {
        List<Trade> trades = status == null
                ? tradeService.listForAccount(accountId)
                : tradeService.listForAccountByStatus(accountId, status);
        return trades.stream().map(TradeResponse::from).toList();
    }

    @PostMapping("/{id}/close")
    public TradeResponse close(@PathVariable Long id, @Valid @RequestBody TradeCloseRequest request) {
        Trade trade = tradeService.closeTrade(id, request);
        return TradeResponse.from(trade);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        tradeService.deleteTrade(id);
    }
}
