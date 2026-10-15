# Futures Order Tracker — Backend

A Spring Boot backend for tracking futures trades entered manually or imported from Excel,
computing realized PnL, aggregating it into a daily calendar view, and reporting account
size/equity. Built as a portfolio project to demonstrate backend design with Java, intended
to eventually be containerized and deployed to OpenShift/AWS as a second phase.

## Stack

- Java 21, Spring Boot 3.3 (Web, Data JPA, Validation)
- H2 in-memory database (swap for Postgres later — see note in `application.properties`)
- Apache POI for `.xlsx` parsing
- springdoc-openapi for a live Swagger UI
- JUnit 5 for tests

## Why this domain

A futures tracker gives a believable reason to need: relational data (accounts ↔ trades),
non-trivial business logic (PnL math that differs for longs/shorts, fees, contract point
values), a file-import feature (Excel), and a reporting/aggregation feature (the PnL
calendar) — more interesting to discuss in an interview than another CRUD-only app.

## Running it

```bash
mvn spring-boot:run
```

The API comes up on `http://localhost:8080`. Swagger UI is at `/swagger-ui.html` — the
fastest way to try every endpoint without writing curl commands. The H2 console is at
`/h2-console` (JDBC URL `jdbc:h2:mem:futurestracker`, user `sa`, blank password) if you want
to poke at the raw tables.

```bash
mvn test
```

runs the unit test suite (PnL math: long/short, fees, multi-contract, daily aggregation).

> **A note on this environment:** this project was built and reviewed in a sandboxed
> session without access to Maven Central, so the build/tests above have not been run by
> that assistant session — they should be run once in your own environment to confirm
> before you rely on this project. The code was reviewed by hand for correctness; flag
> anything that doesn't build and it can be fixed quickly.

## Data model

- **Account** — `name`, `startingBalance`. Current equity is never stored; it's always
  computed as `startingBalance + sum(realized PnL of closed trades)`, so it can't drift out
  of sync with the trade history.
- **Trade** — one futures position: `symbol`, `side` (LONG/SHORT), `quantity`, `pointValue`
  (dollar value of one point of movement, captured at creation time), `entryPrice`/`entryTime`,
  `exitPrice`/`exitTime` (null while open), `fees`, `status` (OPEN/CLOSED), `realizedPnl`
  (null until closed), `notes`.

PnL formula (see `PnlService`):

```
realizedPnl = (exitPrice - entryPrice) * quantity * pointValue * (side == SHORT ? -1 : 1) - fees
```

**Scope note:** only *realized* PnL is computed (closed trades). Mark-to-market/unrealized
PnL on open positions would need a live price feed, which is intentionally out of scope here.

## API overview

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/accounts` | Create an account (`name`, `startingBalance`) |
| GET | `/api/accounts` | List accounts |
| GET | `/api/accounts/{id}` | Get one account |
| GET | `/api/accounts/{id}/summary` | **Account size view**: starting balance, total realized PnL, today's PnL, current equity, open/closed counts |
| POST | `/api/trades` | Create a trade manually (open, or already closed if `exitPrice`+`exitTime` given) |
| GET | `/api/trades?accountId=1&status=OPEN` | List trades for an account, optionally filtered by status |
| GET | `/api/trades/{id}` | Get one trade |
| POST | `/api/trades/{id}/close` | Close an open trade (`exitPrice`, `exitTime`, optional extra `fees`) |
| DELETE | `/api/trades/{id}` | Delete a trade |
| GET | `/api/calendar?accountId=1&year=2026&month=10` | **PnL calendar**: every day of the month with `{date, pnl, tradeCount}`, zero-filled |
| POST | `/api/import/excel?accountId=1` (multipart `file`) | **Excel import**: bulk-load trades from a `.xlsx`, with per-row error reporting |

Known symbols get their point value automatically (`ContractSpecService`: ES, MES, NQ, MNQ,
YM, MYM, RTY, M2K, CL, MCL, NG, GC, MGC, SI, ZB, ZN, ZC, ZS). Unknown symbols require an
explicit `pointValue` in the request/row.

## Excel import format

See `samples/trades_template.xlsx` for a ready-to-use example. Header row, any column order,
case-insensitive:

| Column | Required? | Notes |
|---|---|---|
| Symbol | yes | e.g. `ES`, `NQ`, `CL` |
| Side | yes | `LONG`/`SHORT` or `BUY`/`SELL` |
| Quantity | yes | number of contracts |
| EntryPrice | yes | |
| EntryTime | yes | date/time cell, or text like `2026-10-01 09:31` |
| ExitPrice | no | omit (with ExitTime) to import as an OPEN trade |
| ExitTime | no | |
| PointValue | no | overrides the symbol lookup table |
| Fees | no | defaults to 0 |
| Notes | no | |

A row with a bad value doesn't fail the whole import — it's skipped and reported back in the
`errors` list with its row number and reason, so one typo doesn't cost you the rest of the file.

```bash
curl -F "file=@samples/trades_template.xlsx" "http://localhost:8080/api/import/excel?accountId=1"
```

## Example walkthrough

```bash
# 1. Create an account
curl -X POST localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{"name":"Main","startingBalance":25000}'

# 2. Import sample trades into account 1
curl -F "file=@samples/trades_template.xlsx" "localhost:8080/api/import/excel?accountId=1"

# 3. Check account size / equity
curl localhost:8080/api/accounts/1/summary

# 4. Check October 2026's PnL calendar
curl "localhost:8080/api/calendar?accountId=1&year=2026&month=10"
```

## Design decisions worth mentioning in an interview

- **PnL math lives in exactly one place** (`PnlService`) — trade-close, account summary, and
  the calendar endpoint all call the same method rather than each re-deriving the formula.
- **Equity is derived, never stored** — avoids a whole class of bugs where a cached balance
  drifts from what the trade history actually says.
- **Point value is snapshotted onto the trade at creation time**, not looked up live on every
  read, so historical trades stay accurate even if the reference table changes later.
- **Excel import reuses the same `TradeService.createTrade()` path as manual entry** — every
  row gets the same validation and PnL calculation, and a bad row is isolated instead of
  aborting the whole batch.
- **DB is swappable** — H2 now, Postgres later, by changing four lines in
  `application.properties`; no service or controller code depends on which database is behind
  Spring Data JPA.

## Next steps (the plan beyond this backend)

1. Dockerize (backend + Postgres via `docker-compose`).
2. GitHub Actions CI (build, test, lint).
3. React frontend: calendar heatmap, trade log table, account equity chart, CSV/Excel upload form.
4. Deploy to OpenShift (manifests/Helm chart) with Postgres on AWS RDS.
5. CD pipeline, then observability (structured logs, health/metrics endpoints).
