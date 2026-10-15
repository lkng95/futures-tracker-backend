package com.lk.futurestracker.service;

import com.lk.futurestracker.dto.AccountRequest;
import com.lk.futurestracker.dto.AccountSummaryResponse;
import com.lk.futurestracker.exception.ResourceNotFoundException;
import com.lk.futurestracker.model.Account;
import com.lk.futurestracker.model.Trade;
import com.lk.futurestracker.model.TradeStatus;
import com.lk.futurestracker.repository.AccountRepository;
import com.lk.futurestracker.repository.TradeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TradeRepository tradeRepository;
    private final PnlService pnlService;

    public AccountService(AccountRepository accountRepository,
                           TradeRepository tradeRepository,
                           PnlService pnlService) {
        this.accountRepository = accountRepository;
        this.tradeRepository = tradeRepository;
        this.pnlService = pnlService;
    }

    @Transactional
    public Account createAccount(AccountRequest req) {
        Account account = Account.builder()
                .name(req.getName())
                .startingBalance(req.getStartingBalance())
                .build();
        return accountRepository.save(account);
    }

    public List<Account> listAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccountOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("No account with id " + accountId));
    }

    /** Current equity = starting balance + realized PnL across every closed trade, ever. */
    public AccountSummaryResponse getSummary(Long accountId) {
        Account account = getAccountOrThrow(accountId);

        List<Trade> closedTrades = tradeRepository.findByAccountIdAndStatus(accountId, TradeStatus.CLOSED);
        List<Trade> openTrades = tradeRepository.findByAccountIdAndStatus(accountId, TradeStatus.OPEN);

        BigDecimal totalRealizedPnl = pnlService.sumRealizedPnl(closedTrades);
        BigDecimal currentEquity = account.getStartingBalance().add(totalRealizedPnl);

        LocalDate today = LocalDate.now();
        List<Trade> todaysClosedTrades = closedTrades.stream()
                .filter(t -> t.getExitTime() != null && t.getExitTime().toLocalDate().equals(today))
                .toList();
        BigDecimal todayPnl = pnlService.sumRealizedPnl(todaysClosedTrades);

        return new AccountSummaryResponse(
                account.getId(),
                account.getName(),
                account.getStartingBalance(),
                totalRealizedPnl,
                todayPnl,
                currentEquity,
                openTrades.size(),
                closedTrades.size()
        );
    }
}
