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

    @Value("${gemini.api.model:gemini-1.5-flash}")
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

        // If Gemini API Key is configured, attempt live LLM generation
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                String geminiAnswer = callGeminiApi(question, contextText);
                if (geminiAnswer != null && !geminiAnswer.isBlank()) {
                    return new ChatResponse(geminiAnswer, sourceIds, true);
                }
            } catch (Exception e) {
                logger.warn("Gemini API call failed or rate-limited. Using intelligent RAG response synthesizer.", e);
            }
        }

        // Intelligent Fallback Synthesis when API Key is missing or rate-limited
        String synthesizedAnswer = synthesizeFallbackAnswer(question, contextChunks);
        return new ChatResponse(synthesizedAnswer, sourceIds, false);
    }

    private String callGeminiApi(String question, String contextText) throws Exception {
        String systemPrompt = """
                You are the AI Virtual Assistant for Abhishek Mandloi's personal portfolio website.
                
                Guidelines:
                1. For greetings (e.g. "Hi", "Hello", "Hey"), respond warmly and professionally as Abhishek's virtual assistant.
                2. For specific questions (e.g. "mobile", "phone", "skills", "experience", "education"), provide a clear, concise, direct answer.
                3. Answer based on the provided context below. Be polite, friendly, and natural.
                
                Retrieved Portfolio Context:
                %s
                """.formatted(contextText);

        Map<String, Object> payload = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of("text", systemPrompt + "\n\nUser Prompt: " + question)
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "temperature", 0.4,
                        "maxOutputTokens", 400
                )
        );

        String jsonBody = objectMapper.writeValueAsString(payload);

        // Candidate model endpoints to attempt in order
        List<String> modelsToTry = List.of(modelName, "gemini-1.5-flash", "gemini-1.5-flash-latest", "gemini-2.0-flash", "gemini-1.5-pro");

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
                    logger.info("Successfully generated response using Gemini model: {}", currentModel);
                    return textNode.asText().trim();
                }
            } else {
                logger.warn("Model {} returned HTTP {}: {}", currentModel, response.statusCode(), response.body());
            }
        }

        return null;
    }

    private String synthesizeFallbackAnswer(String question, List<ResumeChunk> chunks) {
        String lowerQ = question.trim().toLowerCase();

        // 1. Handle Greetings
        if (lowerQ.matches("^(hi|hello|hey|greetings|hola|good morning|good afternoon|good evening).*") || lowerQ.length() <= 3) {
            return "Hello! 👋 Welcome to Abhishek Mandloi's portfolio. I'm his AI Assistant. Feel free to ask me about Abhishek's experience in Java, Spring Boot, Big Data streaming (~650M records/day), or how to contact him!";
        }

        // 2. Handle Contact / Mobile / Phone
        if (lowerQ.contains("mobile") || lowerQ.contains("phone") || lowerQ.contains("number") || lowerQ.contains("call") || lowerQ.contains("contact")) {
            return "📱 You can reach **Abhishek Mandloi** directly at **+91 6265494851** or via email at **abhimandloi111@gmail.com**. You can also connect with him on [LinkedIn](https://www.linkedin.com/in/abhishek-mandloi-8412ba1b2/).";
        }

        // 3. Handle Education
        if (lowerQ.contains("education") || lowerQ.contains("degree") || lowerQ.contains("college") || lowerQ.contains("university") || lowerQ.contains("school") || lowerQ.contains("10th") || lowerQ.contains("12th")) {
            return "🎓 **Abhishek Mandloi's Education:**\n\n" +
                   "• **B.Tech in Computer Science**: Sage University, Indore (2018 – 2022)\n" +
                   "• **12th Standard**: Jawahar Navodaya Vidyalaya, Indore (2017 – 2018)\n" +
                   "• **10th Standard**: Jawahar Navodaya Vidyalaya, Indore (2015 – 2016)";
        }

        // 4. Handle Experience / Impetus / Projects
        if (lowerQ.contains("metatrail") || lowerQ.contains("impetus") || lowerQ.contains("project") || lowerQ.contains("experience") || lowerQ.contains("work")) {
            return "💼 **Abhishek Mandloi's Experience Highlights:**\n\n" +
                   "• **Current Role**: Senior Software Engineer at **Impetus Technologies** (March 2022 – Present).\n" +
                   "• **Metatrail Project**: Built real-time streaming pipeline processing **~650 million social media records/day** using Core Java, Spring Boot, Kafka, HBase, and Gathr platform on HDFS/YARN, achieving a **60% data retrieval performance gain**.\n" +
                   "• **Generative AI Chatbot**: Built an AWS + Google Gemini RAG assistant that reduced call center query resolution time by **70%**.";
        }

        // 5. Default natural response for other questions
        if (chunks.isEmpty()) {
            return "Abhishek Mandloi is a Senior Software Engineer with 4+ years of experience in Java, Spring Boot, Big Data engineering, and AI solutions based in Indore, India. You can reach him at abhimandloi111@gmail.com or +91 6265494851.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Here is what I found regarding your question about Abhishek Mandloi:\n\n");

        for (ResumeChunk chunk : chunks) {
            sb.append("• ").append(chunk.content()).append("\n\n");
        }

        sb.append("Feel free to ask for more details or reach out to Abhishek at **abhimandloi111@gmail.com**!");
        return sb.toString().trim();
    }
}
