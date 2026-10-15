package com.lk.futurestracker.service;

import com.lk.futurestracker.dto.ImportResultResponse;
import com.lk.futurestracker.dto.TradeRequest;
import com.lk.futurestracker.exception.ImportException;
import com.lk.futurestracker.model.TradeSide;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses an uploaded .xlsx of trade history into Trade records via TradeService, so every
 * row goes through the exact same validation, point-value resolution and PnL calculation
 * as a manually-entered trade.
 *
 * Expected header row (case-insensitive, any column order): Symbol, Side, Quantity,
 * EntryPrice, EntryTime, ExitPrice, ExitTime, PointValue, Fees, Notes.
 * Required: Symbol, Side, Quantity, EntryPrice, EntryTime. Everything else is optional —
 * a row with no ExitPrice/ExitTime is imported as an OPEN trade.
 *
 * A bad row never aborts the whole file: it's recorded as a per-row error and the import
 * continues, so a single typo in row 47 doesn't cost you the other 200 rows.
 */
@Service
public class ExcelImportService {

    private static final List<String> REQUIRED_COLUMNS =
            List.of("SYMBOL", "SIDE", "QUANTITY", "ENTRYPRICE", "ENTRYTIME");

    private static final List<DateTimeFormatter> TEXT_DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm")
    );

    private final TradeService tradeService;

    public ExcelImportService(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    public ImportResultResponse importTrades(InputStream xlsxStream, Long accountId) {
        // Workbook.close() itself declares `throws IOException`, so it has to be inside the
        // same try/catch as the (also IOException-throwing) WorkbookFactory.create() call —
        // splitting them into a separate try-with-resources left that close()-time exception
        // uncaught, which is what the compiler flagged.
        try (Workbook workbook = WorkbookFactory.create(xlsxStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() == 0) {
                throw new ImportException("Sheet is empty");
            }

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            Map<String, Integer> columnIndex = buildColumnIndex(headerRow);

            for (String required : REQUIRED_COLUMNS) {
                if (!columnIndex.containsKey(required)) {
                    throw new ImportException("Missing required column: " + required
                            + " (required columns: " + REQUIRED_COLUMNS + ")");
                }
            }

            int imported = 0;
            List<ImportResultResponse.RowError> errors = new ArrayList<>();

            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowBlank(row)) {
                    continue;
                }
                int displayRowNumber = r + 1; // 1-indexed, matches what the user sees in Excel
                try {
                    TradeRequest req = rowToTradeRequest(row, columnIndex, accountId);
                    tradeService.createTrade(req);
                    imported++;
                } catch (Exception e) {
                    errors.add(new ImportResultResponse.RowError(displayRowNumber, e.getMessage()));
                }
            }

            return new ImportResultResponse(imported, errors.size(), errors);
        } catch (IOException e) {
            throw new ImportException("Could not read uploaded file as an .xlsx workbook", e);
        }
    }

    private Map<String, Integer> buildColumnIndex(Row headerRow) {
        Map<String, Integer> index = new HashMap<>();
        for (Cell cell : headerRow) {
            String header = readString(cell);
            if (header != null && !header.isBlank()) {
                index.put(header.trim().toUpperCase(), cell.getColumnIndex());
            }
        }
        return index;
    }

    private boolean isRowBlank(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.BLANK && readString(cell) != null && !readString(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private TradeRequest rowToTradeRequest(Row row, Map<String, Integer> col, Long accountId) {
        TradeRequest req = new TradeRequest();
        req.setAccountId(accountId);
        req.setSymbol(requireString(row, col, "SYMBOL"));
        req.setSide(parseSide(requireString(row, col, "SIDE")));
        req.setQuantity(requireDecimal(row, col, "QUANTITY").intValueExact());
        req.setEntryPrice(requireDecimal(row, col, "ENTRYPRICE"));
        req.setEntryTime(requireDateTime(row, col, "ENTRYTIME"));

        req.setExitPrice(optionalDecimal(row, col, "EXITPRICE"));
        req.setExitTime(optionalDateTime(row, col, "EXITTIME"));
        req.setPointValue(optionalDecimal(row, col, "POINTVALUE"));
        BigDecimal fees = optionalDecimal(row, col, "FEES");
        req.setFees(fees == null ? BigDecimal.ZERO : fees);
        req.setNotes(optionalString(row, col, "NOTES"));

        return req;
    }

    private TradeSide parseSide(String raw) {
        String v = raw.trim().toUpperCase();
        return switch (v) {
            case "LONG", "BUY", "B" -> TradeSide.LONG;
            case "SHORT", "SELL", "S" -> TradeSide.SHORT;
            default -> throw new IllegalArgumentException(
                    "Unrecognized side '" + raw + "' (expected LONG/SHORT or BUY/SELL)");
        };
    }

    // --- cell readers -------------------------------------------------------

    private Cell getCell(Row row, Map<String, Integer> col, String name) {
        Integer idx = col.get(name);
        return idx == null ? null : row.getCell(idx);
    }

    private String requireString(Row row, Map<String, Integer> col, String name) {
        String v = optionalString(row, col, name);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Missing value for required column " + name);
        }
        return v;
    }

    private String optionalString(Row row, Map<String, Integer> col, String name) {
        return readString(getCell(row, col, name));
    }

    private BigDecimal requireDecimal(Row row, Map<String, Integer> col, String name) {
        BigDecimal v = optionalDecimal(row, col, name);
        if (v == null) {
            throw new IllegalArgumentException("Missing or non-numeric value for required column " + name);
        }
        return v;
    }

    private BigDecimal optionalDecimal(Row row, Map<String, Integer> col, String name) {
        Cell cell = getCell(row, col, name);
        if (cell == null) return null;
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING -> cell.getStringCellValue().isBlank() ? null : new BigDecimal(cell.getStringCellValue().trim());
                case FORMULA -> BigDecimal.valueOf(cell.getNumericCellValue());
                default -> null;
            };
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Column " + name + " is not a valid number");
        }
    }

    private LocalDateTime requireDateTime(Row row, Map<String, Integer> col, String name) {
        LocalDateTime v = optionalDateTime(row, col, name);
        if (v == null) {
            throw new IllegalArgumentException("Missing or unparseable value for required column " + name);
        }
        return v;
    }

    private LocalDateTime optionalDateTime(Row row, Map<String, Integer> col, String name) {
        Cell cell = getCell(row, col, name);
        if (cell == null) return null;

        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue();
        }
        String text = readString(cell);
        if (text == null || text.isBlank()) return null;

        for (DateTimeFormatter fmt : TEXT_DATE_FORMATS) {
            try {
                return LocalDateTime.parse(text.trim(), fmt);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        throw new IllegalArgumentException(
                "Column " + name + " value '" + text + "' isn't a recognized date/time format");
    }

    private String readString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }
}
