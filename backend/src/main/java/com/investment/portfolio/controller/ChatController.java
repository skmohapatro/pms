package com.investment.portfolio.controller;

import com.investment.portfolio.dto.ChatRequestDTO;
import com.investment.portfolio.dto.ChatResponseDTO;
import com.investment.portfolio.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponseDTO> chat(@RequestBody ChatRequestDTO request) {
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ChatResponseDTO.error("Message cannot be empty"));
        }
        
        ChatResponseDTO response = chatService.chat(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/models")
    public ResponseEntity<List<String>> getModels() {
        return ResponseEntity.ok(chatService.getAvailableModels());
    }

    @GetMapping("/context")
    public ResponseEntity<Map<String, Object>> getContext() {
        return ResponseEntity.ok(chatService.getContextSummary());
    }
}
