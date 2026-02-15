package com.investment.portfolio.dto;

public class UploadResultDTO {
    private int totalRows;
    private int successRows;
    private int failedRows;
    private String message;

    public UploadResultDTO() {}

    public UploadResultDTO(int totalRows, int successRows, int failedRows, String message) {
        this.totalRows = totalRows;
        this.successRows = successRows;
        this.failedRows = failedRows;
        this.message = message;
    }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public int getSuccessRows() { return successRows; }
    public void setSuccessRows(int successRows) { this.successRows = successRows; }

    public int getFailedRows() { return failedRows; }
    public void setFailedRows(int failedRows) { this.failedRows = failedRows; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
