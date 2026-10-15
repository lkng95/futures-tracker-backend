package com.lk.futurestracker.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Reference table of dollar-value-per-full-point for common futures contracts.
 *
 * This is deliberately a simple in-memory map rather than a DB table: point values are
 * exchange-defined constants that almost never change, so there's no real need to make
 * them editable data. If a symbol isn't listed, the caller supplies pointValue manually
 * (see TradeRequest) — the lookup here is a convenience default, not a hard dependency.
 *
 * Values are approximate/standard full-contract point values for portfolio-demo purposes;
 * double-check exact specs (and consider micro vs. standard contracts) before using this
 * for anything beyond a demo.
 */
@Service
public class ContractSpecService {

    private static final Map<String, BigDecimal> POINT_VALUES = Map.ofEntries(
            // Equity index futures
            Map.entry("ES", new BigDecimal("50")),      // E-mini S&P 500
            Map.entry("MES", new BigDecimal("5")),      // Micro E-mini S&P 500
            Map.entry("NQ", new BigDecimal("20")),       // E-mini Nasdaq-100
            Map.entry("MNQ", new BigDecimal("2")),       // Micro E-mini Nasdaq-100
            Map.entry("YM", new BigDecimal("5")),        // E-mini Dow
            Map.entry("MYM", new BigDecimal("0.50")),    // Micro E-mini Dow
            Map.entry("RTY", new BigDecimal("50")),      // E-mini Russell 2000
            Map.entry("M2K", new BigDecimal("5")),       // Micro E-mini Russell 2000

            // Energy
            Map.entry("CL", new BigDecimal("1000")),     // Crude Oil
            Map.entry("MCL", new BigDecimal("100")),     // Micro Crude Oil
            Map.entry("NG", new BigDecimal("10000")),    // Natural Gas

            // Metals
            Map.entry("GC", new BigDecimal("100")),      // Gold
            Map.entry("MGC", new BigDecimal("10")),      // Micro Gold
            Map.entry("SI", new BigDecimal("5000")),     // Silver

            // Rates
            Map.entry("ZB", new BigDecimal("1000")),     // 30-Year T-Bond
            Map.entry("ZN", new BigDecimal("1000")),     // 10-Year T-Note

            // Agriculture
            Map.entry("ZC", new BigDecimal("50")),       // Corn
            Map.entry("ZS", new BigDecimal("50"))        // Soybeans
    );

    public Optional<BigDecimal> lookup(String symbol) {
        if (symbol == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(POINT_VALUES.get(symbol.trim().toUpperCase()));
    }

    public Map<String, BigDecimal> allKnownSpecs() {
        return POINT_VALUES;
    }
}
