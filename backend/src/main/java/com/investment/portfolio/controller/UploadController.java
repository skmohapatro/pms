package com.investment.portfolio.controller;

import com.investment.portfolio.dto.UploadResultDTO;
import com.investment.portfolio.service.ExcelParserService;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final ExcelParserService excelParserService;

    public UploadController(ExcelParserService excelParserService) {
        this.excelParserService = excelParserService;
    }

    @PostMapping("/sheets")
    public ResponseEntity<List<String>> detectSheets(@RequestParam("file") MultipartFile file) {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            List<String> sheets = new ArrayList<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                sheets.add(workbook.getSheetName(i));
            }
            return ResponseEntity.ok(sheets);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<UploadResultDTO> uploadExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sheets", required = false) Set<String> sheets) {
        try {
            UploadResultDTO result = excelParserService.parseAndStore(file, sheets);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new UploadResultDTO(0, 0, 0, "Error: " + e.getMessage()));
        }
    }
}
