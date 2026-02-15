package com.investment.portfolio.service;

import com.investment.portfolio.dto.UploadResultDTO;
import com.investment.portfolio.entity.PurchaseDateWise;
import com.investment.portfolio.repository.PurchaseDateWiseRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ExcelParserService {

    private static final Logger log = LoggerFactory.getLogger(ExcelParserService.class);

    private final PurchaseDateWiseRepository purchaseRepo;
    private final AggregationService aggregationService;

    public ExcelParserService(PurchaseDateWiseRepository purchaseRepo, AggregationService aggregationService) {
        this.purchaseRepo = purchaseRepo;
        this.aggregationService = aggregationService;
    }

    public UploadResultDTO parseAndStore(MultipartFile file) throws Exception {
        int totalRows = 0;
        int successRows = 0;
        int failedRows = 0;
        List<PurchaseDateWise> records = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheet("Purchase Date Wise");
            if (sheet == null) {
                // Try case-insensitive search
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    if (workbook.getSheetName(i).trim().equalsIgnoreCase("Purchase Date Wise")) {
                        sheet = workbook.getSheetAt(i);
                        break;
                    }
                }
            }

            if (sheet == null) {
                throw new RuntimeException("Worksheet 'Purchase Date Wise' not found in the uploaded file.");
            }

            // Find header row and column indices
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("Header row is empty.");
            }

            int dateCol = -1, companyCol = -1, qtyCol = -1, priceCol = -1, investmentCol = -1;

            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                Cell cell = headerRow.getCell(c);
                if (cell == null) continue;
                String header = cell.getStringCellValue().trim().toLowerCase();
                switch (header) {
                    case "date" -> dateCol = c;
                    case "company" -> companyCol = c;
                    case "quantity" -> qtyCol = c;
                    case "price" -> priceCol = c;
                    case "investment" -> investmentCol = c;
                }
            }

            if (dateCol == -1 || companyCol == -1 || qtyCol == -1 || priceCol == -1 || investmentCol == -1) {
                throw new RuntimeException("Missing required columns. Expected: Date, Company, Quantity, Price, Investment");
            }

            // Clear existing data (replace strategy)
            purchaseRepo.deleteAll();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                totalRows++;

                try {
                    LocalDate date = parseDate(row.getCell(dateCol));
                    String company = parseCellAsString(row.getCell(companyCol));
                    Double quantity = parseCellAsDouble(row.getCell(qtyCol));
                    Double price = parseCellAsDouble(row.getCell(priceCol));
                    Double investment = parseCellAsDouble(row.getCell(investmentCol));

                    if (date == null || company == null || company.isBlank() || quantity == null || price == null || investment == null) {
                        log.warn("Row {} skipped - date:{} company:{} qty:{} price:{} inv:{}",
                                r, date, company, quantity, price, investment);
                        if (r <= 3) {
                            Cell dc = row.getCell(dateCol);
                            Cell cc = row.getCell(companyCol);
                            Cell qc = row.getCell(qtyCol);
                            Cell pc = row.getCell(priceCol);
                            Cell ic = row.getCell(investmentCol);
                            log.warn("  Cell types - date:{} company:{} qty:{} price:{} inv:{}",
                                    dc != null ? dc.getCellType() : "null",
                                    cc != null ? cc.getCellType() : "null",
                                    qc != null ? qc.getCellType() : "null",
                                    pc != null ? pc.getCellType() : "null",
                                    ic != null ? ic.getCellType() : "null");
                        }
                        failedRows++;
                        continue;
                    }

                    records.add(new PurchaseDateWise(date, company.trim(), quantity, price, investment));
                    successRows++;
                } catch (Exception e) {
                    log.error("Row {} exception: {}", r, e.getMessage());
                    failedRows++;
                }
            }

            if (!records.isEmpty()) {
                purchaseRepo.saveAll(records);
            }

            // Rebuild aggregation
            aggregationService.rebuildAggregation();
        }

        return new UploadResultDTO(totalRows, successRows, failedRows,
                "Upload complete. " + successRows + " rows imported, " + failedRows + " rows skipped.");
    }

    private LocalDate parseDate(Cell cell) {
        if (cell == null) return null;
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        try {
            if (type == CellType.NUMERIC) {
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                }
                // Might be a numeric date serial
                Date date = DateUtil.getJavaDate(cell.getNumericCellValue());
                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } else if (type == CellType.STRING) {
                String dateStr = cell.getStringCellValue().trim();
                // Try ISO format yyyy-MM-dd
                try {
                    return LocalDate.parse(dateStr);
                } catch (Exception e) {
                    // Try dd-MM-yyyy or dd/MM/yyyy
                    String[] parts = dateStr.split("[/\\-]");
                    if (parts.length == 3) {
                        int day = Integer.parseInt(parts[0]);
                        int month = Integer.parseInt(parts[1]);
                        int year = Integer.parseInt(parts[2]);
                        return LocalDate.of(year, month, day);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Date parse error for cell value: {}", cell, e);
        }
        return null;
    }

    private String parseCellAsString(Cell cell) {
        if (cell == null) return null;
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        if (type == CellType.STRING) return cell.getStringCellValue();
        if (type == CellType.NUMERIC) return String.valueOf(cell.getNumericCellValue());
        if (type == CellType.BOOLEAN) return String.valueOf(cell.getBooleanCellValue());
        return null;
    }

    private Double parseCellAsDouble(Cell cell) {
        if (cell == null) return null;
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        try {
            if (type == CellType.NUMERIC) return cell.getNumericCellValue();
            if (type == CellType.STRING) {
                String val = cell.getStringCellValue().trim();
                if (!val.isEmpty()) return Double.parseDouble(val);
            }
        } catch (Exception e) {
            log.warn("Numeric parse error for cell: {}", cell, e);
        }
        return null;
    }
}
