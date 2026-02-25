package com.investment.portfolio.service;

import com.investment.portfolio.dto.ChatRequestDTO;
import com.investment.portfolio.dto.ChatResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class ChatService {

    @Autowired
    private ChatContextService contextService;

    @Value("${chat.backend.url:http://localhost:5000}")
    private String chatBackendUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public ChatResponseDTO chat(ChatRequestDTO request) {
        try {
            String context = contextService.buildDynamicContext();
            
            List<Map<String, String>> messages = new ArrayList<>();
            
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", context);
            messages.add(systemMessage);

            if (request.getHistory() != null) {
                for (ChatRequestDTO.ChatMessage historyMsg : request.getHistory()) {
                    Map<String, String> msg = new HashMap<>();
                    msg.put("role", historyMsg.getRole());
                    msg.put("content", historyMsg.getContent());
                    messages.add(msg);
                }
            }

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", request.getMessage());
            messages.add(userMessage);

            Map<String, Object> payload = new HashMap<>();
            payload.put("messages", messages);
            payload.put("model", request.getModel() != null ? request.getModel() : "gemma-3-27b-it");
            payload.put("temperature", 0.7);
            payload.put("max_tokens", 2000);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                chatBackendUrl + "/api/chat",
                entity,
                Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String message = (String) body.get("message");
                String model = (String) body.get("model");

                ChatResponseDTO chatResponse = new ChatResponseDTO();
                chatResponse.setMessage(message);
                chatResponse.setModel(model);
                chatResponse.setSuccess(true);
                return chatResponse;
            } else {
                return ChatResponseDTO.error("Failed to get response from AI service");
            }

        } catch (Exception e) {
            return ChatResponseDTO.error("Error communicating with AI service: " + e.getMessage());
        }
    }

    public List<String> getAvailableModels() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                chatBackendUrl + "/api/models",
                Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (List<String>) response.getBody().get("models");
            }
        } catch (Exception e) {
            // Return default models if chat-backend is not available
        }
        return Arrays.asList("gemma-3-27b-it", "pixtral-12b-2409");
    }

    public Map<String, Object> getContextSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("portfolioSummary", contextService.getPortfolioSummary());
        summary.put("availableModels", getAvailableModels());
        return summary;
    }
}
