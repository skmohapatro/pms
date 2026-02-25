package com.investment.portfolio.dto;

import java.util.List;

public class ChatRequestDTO {
    private String message;
    private String model;
    private List<ChatMessage> history;

    public static class ChatMessage {
        private String role;
        private String content;

        public ChatMessage() {}

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public List<ChatMessage> getHistory() { return history; }
    public void setHistory(List<ChatMessage> history) { this.history = history; }
}
