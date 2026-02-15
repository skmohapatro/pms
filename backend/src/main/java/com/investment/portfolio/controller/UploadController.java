package com.investment.portfolio.controller;

import com.investment.portfolio.dto.UploadResultDTO;
import com.investment.portfolio.service.ExcelParserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final ExcelParserService excelParserService;

    public UploadController(ExcelParserService excelParserService) {
        this.excelParserService = excelParserService;
    }

    @PostMapping
    public ResponseEntity<UploadResultDTO> uploadExcel(@RequestParam("file") MultipartFile file) {
        try {
            UploadResultDTO result = excelParserService.parseAndStore(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new UploadResultDTO(0, 0, 0, "Error: " + e.getMessage()));
        }
    }
}
