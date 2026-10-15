package com.lk.futurestracker.service;

import com.lk.futurestracker.dto.TradeCloseRequest;
import com.lk.futurestracker.dto.TradeRequest;
import com.lk.futurestracker.exception.InvalidTradeStateException;
import com.lk.futurestracker.exception.ResourceNotFoundException;
import com.lk.futurestracker.model.Account;
import com.lk.futurestracker.model.Trade;
import com.lk.futurestracker.model.TradeStatus;
import com.lk.futurestracker.repository.AccountRepository;
import com.lk.futurestracker.repository.TradeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TradeService {

    private final TradeRepository tradeRepository;
    private final AccountRepository accountRepository;
    private final PnlService pnlService;
    private final ContractSpecService contractSpecService;

    public TradeService(TradeRepository tradeRepository,
                         AccountRepository accountRepository,
                         PnlService pnlService,
                         ContractSpecService contractSpecService) {
        this.tradeRepository = tradeRepository;
        this.accountRepository = accountRepository;
        this.pnlService = pnlService;
        this.contractSpecService = contractSpecService;
    }

    @Transactional
    public Trade createTrade(TradeRequest req) {
        Account account = accountRepository.findById(req.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("No account with id " + req.getAccountId()));

        BigDecimal pointValue = resolvePointValue(req.getSymbol(), req.getPointValue());
        boolean closingOnCreate = req.getExitPrice() != null && req.getExitTime() != null;

        Trade trade = Trade.builder()
                .account(account)
                .symbol(req.getSymbol().trim().toUpperCase())
                .side(req.getSide())
                .quantity(req.getQuantity())
                .pointValue(pointValue)
                .entryPrice(req.getEntryPrice())
                .entryTime(req.getEntryTime())
                .exitPrice(req.getExitPrice())
                .exitTime(req.getExitTime())
                .fees(req.getFees() == null ? BigDecimal.ZERO : req.getFees())
                .status(closingOnCreate ? TradeStatus.CLOSED : TradeStatus.OPEN)
                .notes(req.getNotes())
                .build();

        if (closingOnCreate) {
            trade.setRealizedPnl(pnlService.calculateRealizedPnl(trade));
        }

        return tradeRepository.save(trade);
    }

    @Transactional
    public Trade closeTrade(Long tradeId, TradeCloseRequest req) {
        Trade trade = getTradeOrThrow(tradeId);

        if (trade.getStatus() == TradeStatus.CLOSED) {
            throw new InvalidTradeStateException("Trade " + tradeId + " is already closed");
        }

        trade.setExitPrice(req.getExitPrice());
        trade.setExitTime(req.getExitTime());
        if (req.getFees() != null) {
            BigDecimal existing = trade.getFees() == null ? BigDecimal.ZERO : trade.getFees();
            trade.setFees(existing.add(req.getFees()));
        }
        trade.setStatus(TradeStatus.CLOSED);
        trade.setRealizedPnl(pnlService.calculateRealizedPnl(trade));

        return tradeRepository.save(trade);
    }

    @Transactional
    public void deleteTrade(Long tradeId) {
        Trade trade = getTradeOrThrow(tradeId);
        tradeRepository.delete(trade);
    }

    public Trade getTradeOrThrow(Long tradeId) {
        return tradeRepository.findById(tradeId)
                .orElseThrow(() -> new ResourceNotFoundException("No trade with id " + tradeId));
    }

    public List<Trade> listForAccount(Long accountId) {
        ensureAccountExists(accountId);
        return tradeRepository.findByAccountIdOrderByEntryTimeDesc(accountId);
    }

    public List<Trade> listForAccountByStatus(Long accountId, TradeStatus status) {
        ensureAccountExists(accountId);
        return tradeRepository.findByAccountIdAndStatus(accountId, status);
    }

    /** Resolves pointValue: explicit value wins, otherwise falls back to the symbol lookup table. */
    private BigDecimal resolvePointValue(String symbol, BigDecimal explicitPointValue) {
        if (explicitPointValue != null) {
            return explicitPointValue;
        }
        return contractSpecService.lookup(symbol)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown symbol '" + symbol + "' and no pointValue provided. " +
                        "Either supply pointValue explicitly or use a known symbol."));
    }

    private void ensureAccountExists(Long accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("No account with id " + accountId);
        }
    }
}
