package com.lk.futurestracker.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ImportResultResponse {
    private final int importedCount;
    private final int failedCount;
    private final List<RowError> errors;

    public record RowError(int rowNumber, String message) {
    }
}
