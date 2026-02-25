package com.investment.portfolio.service;

import com.investment.portfolio.dto.UploadResultDTO;
import com.investment.portfolio.entity.Dividend;
import com.investment.portfolio.entity.PurchaseDateWise;
import com.investment.portfolio.entity.RealizedPnL;
import com.investment.portfolio.repository.DividendRepository;
import com.investment.portfolio.repository.PurchaseDateWiseRepository;
import com.investment.portfolio.repository.RealizedPnLRepository;
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
import java.util.Set;

@Service
public class ExcelParserService {

    private static final Logger log = LoggerFactory.getLogger(ExcelParserService.class);

    private final PurchaseDateWiseRepository purchaseRepo;
    private final AggregationService aggregationService;
    private final DividendRepository dividendRepo;
    private final RealizedPnLRepository realizedPnLRepo;

    public ExcelParserService(PurchaseDateWiseRepository purchaseRepo, AggregationService aggregationService,
                              DividendRepository dividendRepo, RealizedPnLRepository realizedPnLRepo) {
        this.purchaseRepo = purchaseRepo;
        this.aggregationService = aggregationService;
        this.dividendRepo = dividendRepo;
        this.realizedPnLRepo = realizedPnLRepo;
    }

    public UploadResultDTO parseAndStore(MultipartFile file) throws Exception {
        return parseAndStore(file, null);
    }

    public UploadResultDTO parseAndStore(MultipartFile file, Set<String> selectedSheets) throws Exception {
        int totalRows = 0;
        int successRows = 0;
        int failedRows = 0;
        List<PurchaseDateWise> records = new ArrayList<>();

        boolean importPurchase = selectedSheets == null || selectedSheets.stream()
                .anyMatch(s -> s.equalsIgnoreCase("Purchase Date Wise"));
        boolean importDividend = selectedSheets == null || selectedSheets.stream()
                .anyMatch(s -> s.equalsIgnoreCase("Dividend"));
        boolean importPnL = selectedSheets == null || selectedSheets.stream()
                .anyMatch(s -> s.equalsIgnoreCase("RealizedP&L"));

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            if (importPurchase) {
                Sheet sheet = findSheet(workbook, "Purchase Date Wise");
                if (sheet == null) {
                    throw new RuntimeException("Worksheet 'Purchase Date Wise' not found in the uploaded file.");
                }

                Row headerRow = sheet.getRow(0);
                if (headerRow == null) throw new RuntimeException("Header row is empty.");

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

                if (!records.isEmpty()) purchaseRepo.saveAll(records);
                aggregationService.rebuildAggregation();
            } else {
                log.info("Skipping 'Purchase Date Wise' sheet (not selected)");
            }

            // Parse Dividend sheet
            if (importDividend) {
                int[] dividendResult = parseDividendSheet(workbook);
                totalRows += dividendResult[0];
                successRows += dividendResult[1];
                failedRows += dividendResult[2];
            } else {
                log.info("Skipping 'Dividend' sheet (not selected)");
            }

            // Parse RealizedP&L sheet
            if (importPnL) {
                int[] pnlResult = parseRealizedPnLSheet(workbook);
                totalRows += pnlResult[0];
                successRows += pnlResult[1];
                failedRows += pnlResult[2];
            } else {
                log.info("Skipping 'RealizedP&L' sheet (not selected)");
            }
        }

        return new UploadResultDTO(totalRows, successRows, failedRows,
                "Upload complete. " + successRows + " rows imported, " + failedRows + " rows skipped.");
    }

    private int[] parseDividendSheet(Workbook workbook) {
        int total = 0, success = 0, failed = 0;
        Sheet sheet = findSheet(workbook, "Dividend");
        if (sheet == null) {
            log.info("No 'Dividend' sheet found, skipping.");
            return new int[]{0, 0, 0};
        }

        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return new int[]{0, 0, 0};

        int symbolCol = -1, isinCol = -1, exDateCol = -1, qtyCol = -1,
                dpsCol = -1, netAmtCol = -1, fyCol = -1;

        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            Cell cell = headerRow.getCell(c);
            if (cell == null) continue;
            String h = cell.getStringCellValue().trim().toLowerCase();
            switch (h) {
                case "symbol" -> symbolCol = c;
                case "isin" -> isinCol = c;
                case "ex-date" -> exDateCol = c;
                case "quantity" -> qtyCol = c;
                case "dividend per share" -> dpsCol = c;
                case "net dividend amount" -> netAmtCol = c;
                case "fy" -> fyCol = c;
            }
        }

        dividendRepo.deleteAll();
        List<Dividend> records = new ArrayList<>();

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            total++;
            try {
                String symbol = parseCellAsString(symbolCol >= 0 ? row.getCell(symbolCol) : null);
                if (symbol == null || symbol.isBlank()) { failed++; continue; }

                String isin = parseCellAsString(isinCol >= 0 ? row.getCell(isinCol) : null);
                LocalDate exDate = exDateCol >= 0 ? parseDateFlexible(row.getCell(exDateCol)) : null;
                Double qty = qtyCol >= 0 ? parseCellAsDouble(row.getCell(qtyCol)) : null;
                Double dps = dpsCol >= 0 ? parseCellAsDouble(row.getCell(dpsCol)) : null;
                Double netAmt = netAmtCol >= 0 ? parseCellAsDouble(row.getCell(netAmtCol)) : null;
                String fy = fyCol >= 0 ? parseCellAsString(row.getCell(fyCol)) : null;

                records.add(new Dividend(symbol.trim(), isin, exDate, qty, dps, netAmt, fy));
                success++;
            } catch (Exception e) {
                log.error("Dividend row {} error: {}", r, e.getMessage());
                failed++;
            }
        }

        if (!records.isEmpty()) dividendRepo.saveAll(records);
        log.info("Dividend sheet: {} total, {} success, {} failed", total, success, failed);
        return new int[]{total, success, failed};
    }

    private int[] parseRealizedPnLSheet(Workbook workbook) {
        int total = 0, success = 0, failed = 0;
        Sheet sheet = findSheet(workbook, "RealizedP&L");
        if (sheet == null) {
            log.info("No 'RealizedP&L' sheet found, skipping.");
            return new int[]{0, 0, 0};
        }

        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return new int[]{0, 0, 0};

        int symbolCol = -1, isinCol = -1, qtyCol = -1,
                buyCol = -1, sellCol = -1, pnlCol = -1;

        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            Cell cell = headerRow.getCell(c);
            if (cell == null) continue;
            String h = cell.getStringCellValue().trim().toLowerCase();
            switch (h) {
                case "symbol" -> symbolCol = c;
                case "isin" -> isinCol = c;
                case "quantity" -> qtyCol = c;
                case "buy value" -> buyCol = c;
                case "sell value" -> sellCol = c;
                case "realized p&l" -> pnlCol = c;
            }
        }

        realizedPnLRepo.deleteAll();
        List<RealizedPnL> records = new ArrayList<>();

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            total++;
            try {
                String symbol = parseCellAsString(symbolCol >= 0 ? row.getCell(symbolCol) : null);
                if (symbol == null || symbol.isBlank()) { failed++; continue; }

                String isin = parseCellAsString(isinCol >= 0 ? row.getCell(isinCol) : null);
                Double qty = qtyCol >= 0 ? parseCellAsDouble(row.getCell(qtyCol)) : null;
                Double buyVal = buyCol >= 0 ? parseCellAsDouble(row.getCell(buyCol)) : null;
                Double sellVal = sellCol >= 0 ? parseCellAsDouble(row.getCell(sellCol)) : null;
                Double pnl = pnlCol >= 0 ? parseCellAsDouble(row.getCell(pnlCol)) : null;

                records.add(new RealizedPnL(symbol.trim(), isin, qty, buyVal, sellVal, pnl));
                success++;
            } catch (Exception e) {
                log.error("RealizedPnL row {} error: {}", r, e.getMessage());
                failed++;
            }
        }

        if (!records.isEmpty()) realizedPnLRepo.saveAll(records);
        log.info("RealizedP&L sheet: {} total, {} success, {} failed", total, success, failed);
        return new int[]{total, success, failed};
    }

    private Sheet findSheet(Workbook workbook, String name) {
        Sheet sheet = workbook.getSheet(name);
        if (sheet != null) return sheet;
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            if (workbook.getSheetName(i).trim().equalsIgnoreCase(name)) {
                return workbook.getSheetAt(i);
            }
        }
        return null;
    }

    private LocalDate parseDateFlexible(Cell cell) {
        if (cell == null) return null;
        LocalDate d = parseDate(cell);
        if (d != null) return d;
        // Try string "yyyy-MM-dd"
        try {
            String s = cell.getStringCellValue().trim();
            if (!s.isBlank()) return LocalDate.parse(s);
        } catch (Exception ignored) {}
        return null;
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
