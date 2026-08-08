package com.abhishek.portfolio.service;

import com.abhishek.portfolio.dto.ChatResponse;
import com.abhishek.portfolio.model.ResumeChunk;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.0-flash}")
    private String modelName;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String baseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatResponse generateAnswer(String question, List<ResumeChunk> contextChunks) {
        List<String> sourceIds = contextChunks.stream()
                .map(chunk -> chunk.category() + ": " + chunk.id())
                .distinct()
                .collect(Collectors.toList());

        String contextText = contextChunks.stream()
                .map(chunk -> "- [" + chunk.category() + "] " + chunk.content())
                .collect(Collectors.joining("\n"));

        if (apiKey != null && !apiKey.isBlank()) {
            try {
                String geminiAnswer = callGeminiApi(question, contextText);
                if (geminiAnswer != null && !geminiAnswer.isBlank()) {
                    return new ChatResponse(geminiAnswer, sourceIds, true);
                }
            } catch (Exception e) {
                logger.warn("Gemini API call failed or timed out. Falling back to local RAG synthesis.", e);
            }
        }

        // Fallback RAG synthesis when Gemini key is unconfigured or call fails
        String synthesizedAnswer = synthesizeFallbackAnswer(question, contextChunks);
        return new ChatResponse(synthesizedAnswer, sourceIds, false);
    }

    private String callGeminiApi(String question, String contextText) throws Exception {
        String systemPrompt = """
                You are the AI Assistant for Abhishek Mandloi's personal portfolio website.
                Answer visitors' questions accurately, professionally, and concisely based ONLY on the provided context below.
                If the question cannot be answered using the provided context, politely inform the user that you only have information regarding Abhishek's professional experience, skills, projects, and education.
                
                Retrieved Context:
                %s
                """.formatted(contextText);

        Map<String, Object> payload = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of("text", systemPrompt + "\n\nUser Question: " + question)
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "maxOutputTokens", 500
                )
        );

        String jsonBody = objectMapper.writeValueAsString(payload);

        // Candidate model endpoints to attempt in order
        List<String> modelsToTry = List.of(modelName, "gemini-2.0-flash", "gemini-2.5-flash", "gemini-1.5-flash-8b");

        for (String currentModel : modelsToTry) {
            String endpoint = baseUrl + "/" + currentModel + ":generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(12))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode textNode = root.path("candidates")
                        .path(0)
                        .path("content")
                        .path("parts")
                        .path(0)
                        .path("text");
                if (!textNode.isMissingNode()) {
                    logger.info("Successfully received answer from Gemini model: {}", currentModel);
                    return textNode.asText().trim();
                }
            } else {
                logger.warn("Model {} returned status {}: {}", currentModel, response.statusCode(), response.body());
            }
        }

        return null;
    }

    private String synthesizeFallbackAnswer(String question, List<ResumeChunk> chunks) {
        if (chunks.isEmpty()) {
            return "Abhishek Mandloi is a Senior Software Engineer with 4+ years of experience specializing in Java, Spring Boot, Big Data pipelines, and Generative AI. Feel free to ask about his Metatrail project or RAG Chatbot!";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Based on Abhishek's portfolio data:\n\n");

        for (ResumeChunk chunk : chunks) {
            sb.append("• ").append(chunk.content()).append("\n\n");
        }

        sb.append("For more details or direct inquiries, connect with Abhishek at abhimandloi111@gmail.com or on LinkedIn!");
        return sb.toString().trim();
    }
}
