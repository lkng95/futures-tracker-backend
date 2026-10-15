package com.lk.futurestracker.controller;

import com.lk.futurestracker.dto.ImportResultResponse;
import com.lk.futurestracker.exception.ImportException;
import com.lk.futurestracker.service.ExcelImportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final ExcelImportService excelImportService;

    public ImportController(ExcelImportService excelImportService) {
        this.excelImportService = excelImportService;
    }

    /**
     * Upload a .xlsx trade history for one account.
     * curl -F "file=@trades.xlsx" "http://localhost:8080/api/import/excel?accountId=1"
     */
    @PostMapping("/excel")
    public ImportResultResponse importExcel(@RequestParam("file") MultipartFile file,
                                              @RequestParam Long accountId) {
        if (file.isEmpty()) {
            throw new ImportException("Uploaded file is empty");
        }
        try {
            return excelImportService.importTrades(file.getInputStream(), accountId);
        } catch (IOException e) {
            throw new ImportException("Could not read uploaded file", e);
        }
    }
}
