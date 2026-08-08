package com.abhishek.portfolio.controller;

import com.abhishek.portfolio.dto.ChatRequest;
import com.abhishek.portfolio.dto.ChatResponse;
import com.abhishek.portfolio.model.ResumeChunk;
import com.abhishek.portfolio.service.GeminiService;
import com.abhishek.portfolio.service.RagService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class ChatController {

    private final RagService ragService;
    private final GeminiService geminiService;

    public ChatController(RagService ragService, GeminiService geminiService) {
        this.ragService = ragService;
        this.geminiService = geminiService;
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> rootEndpoint() {
        return ResponseEntity.ok(Map.of(
                "service", "Abhishek Mandloi Portfolio Spring Boot RAG Backend",
                "status", "UP",
                "frontendUrl", "http://localhost:4200",
                "healthCheck", "/api/health",
                "chatEndpoint", "/api/chat (POST)"
        ));
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Abhishek Mandloi Portfolio RAG Backend",
                "indexedChunks", ragService.getAllChunks().size()
        ));
    }

    @GetMapping("/api/chat")
    public ResponseEntity<Map<String, Object>> getChatInfo() {
        return ResponseEntity.ok(Map.of(
                "info", "Abhishek Mandloi AI Assistant REST API",
                "method", "POST",
                "exampleBody", Map.of("question", "Tell me about Metatrail project")
        ));
    }

    @PostMapping("/api/chat")
    public ResponseEntity<ChatResponse> processChatQuestion(@Valid @RequestBody ChatRequest request) {
        List<ResumeChunk> retrievedChunks = ragService.retrieveContextChunks(request.question(), 3);
        ChatResponse response = geminiService.generateAnswer(request.question(), retrievedChunks);
        return ResponseEntity.ok(response);
    }
}
