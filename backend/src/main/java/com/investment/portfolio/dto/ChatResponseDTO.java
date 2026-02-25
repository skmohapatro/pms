package com.investment.portfolio.dto;

public class ChatResponseDTO {
    private String message;
    private String model;
    private boolean success;
    private String error;

    public ChatResponseDTO() {}

    public ChatResponseDTO(String message, String model, boolean success) {
        this.message = message;
        this.model = model;
        this.success = success;
    }

    public static ChatResponseDTO error(String error) {
        ChatResponseDTO response = new ChatResponseDTO();
        response.setSuccess(false);
        response.setError(error);
        return response;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
