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
import java.util.regex.Pattern;
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

    // Guardrail pattern to scrub mobile numbers from responses for privacy
    private static final Pattern PHONE_GUARDRAIL_PATTERN = Pattern.compile("(\\+?91[\\-\\s]?)?[6-9]\\d{9}");

    public ChatResponse generateAnswer(String question, List<ResumeChunk> contextChunks) {
        List<String> sourceIds = contextChunks.stream()
                .map(chunk -> chunk.category() + ": " + chunk.id())
                .distinct()
                .collect(Collectors.toList());

        String contextText = contextChunks.stream()
                .map(chunk -> "- [" + chunk.category() + "] " + chunk.content())
                .collect(Collectors.joining("\n"));

        String rawAnswer = null;
        boolean isGemini = false;

        // If Gemini API Key is configured, attempt live LLM generation
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                rawAnswer = callGeminiApi(question, contextText);
                if (rawAnswer != null && !rawAnswer.isBlank()) {
                    isGemini = true;
                }
            } catch (Exception e) {
                logger.warn("Gemini API call failed or rate-limited. Using intelligent RAG response synthesizer.", e);
            }
        }

        // Intelligent Fallback Synthesis when API Key is missing or rate-limited
        if (rawAnswer == null || rawAnswer.isBlank()) {
            rawAnswer = synthesizeFallbackAnswer(question, contextChunks);
            isGemini = false;
        }

        // Apply Privacy Guardrail: Ensure mobile numbers are NEVER returned in response
        String sanitizedAnswer = applyPrivacyGuardrail(rawAnswer);

        return new ChatResponse(sanitizedAnswer, sourceIds, isGemini);
    }

    private String callGeminiApi(String question, String contextText) throws Exception {
        String systemPrompt = """
                You are the official AI Virtual Assistant for Abhishek Mandloi's personal portfolio website.
                
                PRIVACY GUARDRAIL: NEVER reveal or disclose Abhishek's mobile phone number. If asked for phone or mobile number, direct the user to email him at abhimandloi111@gmail.com or connect on LinkedIn.
                
                RESPONSE RULES:
                1. For greetings (e.g. "Hi", "Hello", "Hey"), respond warmly and professionally as Abhishek's virtual assistant.
                2. If the user asks about something NOT mentioned in the provided context (such as marital status, private family details, etc.), reply politely and softly like:
                   "I don't have information about Abhishek's [topic] in my knowledge base. However, I can tell you about his professional experience, awards, Big Data projects, or how to contact him via email."
                3. DO NOT dump raw, unrelated resume text or bullet lists when a question is out of scope or missing from context.
                4. Highlight his awards (Star of the Month, Excellence/Transformational Performance Award) and career growth from fresher to Senior Software Engineer at Impetus Technologies when relevant.
                5. Keep answers clear, friendly, structured, and professional.
                
                Retrieved Portfolio Context:
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
                        "maxOutputTokens", 400
                )
        );

        String jsonBody = objectMapper.writeValueAsString(payload);

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
            return "Hello! 👋 Welcome to Abhishek Mandloi's portfolio. I'm his AI Assistant. Feel free to ask me about Abhishek's experience in Java, Spring Boot, Big Data streaming (~650M records/day), his awards at Impetus Technologies, or how to email him!";
        }

        // 2. Handle Contact / Mobile / Phone (Privacy Guardrail Enforced)
        if (lowerQ.contains("mobile") || lowerQ.contains("phone") || lowerQ.contains("number") || lowerQ.contains("call") || lowerQ.contains("contact")) {
            return "✉️ For direct inquiries, you can reach **Abhishek Mandloi** via email at **abhimandloi111@gmail.com** or connect with him professionally on [LinkedIn](https://www.linkedin.com/in/abhishek-mandloi-8412ba1b2/).";
        }

        // 3. Handle Awards & Achievements
        if (lowerQ.contains("award") || lowerQ.contains("achievement") || lowerQ.contains("star") || lowerQ.contains("excellence") || lowerQ.contains("recognition")) {
            return "🏆 **Abhishek Mandloi's Awards & Recognition at Impetus Technologies:**\n\n" +
                   "• ⭐ **Star of the Month Award**: Received this recognition **multiple times** for consistent high performance and contribution to project goals.\n" +
                   "• 🏅 **Excellence Award (Transformational Performance Award)**: Awarded in recognition of transformational impact and outstanding performance in project delivery.";
        }

        // 4. Handle Hobbies & Passions
        if (lowerQ.contains("hobby") || lowerQ.contains("hobbies") || lowerQ.contains("interest") || lowerQ.contains("free time") || lowerQ.contains("passions")) {
            return "🎨 **Abhishek's Hobbies & Interests:**\n\n" +
                   "Abhishek enjoys exploring emerging AI tools (Google Gemini, GitHub Copilot), experimenting with new software architectures, learning modern cloud technologies, and continuous self-improvement in software engineering.";
        }

        // 5. Handle Career Journey / Growth
        if (lowerQ.contains("journey") || lowerQ.contains("growth") || lowerQ.contains("fresher") || lowerQ.contains("join") || lowerQ.contains("promotion")) {
            return "🚀 **Abhishek's Career Journey:**\n\n" +
                   "Abhishek joined **Impetus Technologies (India) Pvt. Ltd.** as a fresher in **March 2022** and rapidly advanced into a **Senior Software Engineer** role, architecting high-throughput Big Data pipelines (~650M records/day) and production Generative AI applications.";
        }

        // 6. Handle Out-of-Scope / Private Questions
        if (lowerQ.contains("salary") || lowerQ.contains("marital") || lowerQ.contains("age") || lowerQ.contains("family") || lowerQ.contains("personal")) {
            return "I don't have information on Abhishek's private personal details or salary topics in my knowledge base. I can answer questions about his professional experience, awards, education, skills, and projects!";
        }

        // 7. Handle Education
        if (lowerQ.contains("education") || lowerQ.contains("degree") || lowerQ.contains("college") || lowerQ.contains("university") || lowerQ.contains("school") || lowerQ.contains("10th") || lowerQ.contains("12th")) {
            return "🎓 **Abhishek Mandloi's Education:**\n\n" +
                   "• **B.Tech in Computer Science**: Sage University, Indore (2018 – 2022)\n" +
                   "• **12th Standard**: Jawahar Navodaya Vidyalaya, Indore (2017 – 2018)\n" +
                   "• **10th Standard**: Jawahar Navodaya Vidyalaya, Indore (2015 – 2016)";
        }

        // 8. Handle Experience / Impetus / Projects
        if (lowerQ.contains("metatrail") || lowerQ.contains("impetus") || lowerQ.contains("project") || lowerQ.contains("experience") || lowerQ.contains("work")) {
            return "💼 **Abhishek Mandloi's Experience Highlights:**\n\n" +
                   "• **Current Role**: Senior Software Engineer at **Impetus Technologies** (March 2022 – Present).\n" +
                   "• **Metatrail Project**: Built real-time streaming pipeline processing **~650 million social media records/day** using Core Java, Spring Boot, Kafka, HBase, and Gathr platform on HDFS/YARN, achieving a **60% data retrieval performance gain**.\n" +
                   "• **Generative AI Chatbot**: Built an AWS + Google Gemini RAG assistant that reduced call center query resolution time by **70%**.\n" +
                   "• **Awards**: Multiple-time **Star of the Month** & **Excellence (Transformational Performance) Award** winner.";
        }

        // 9. Default soft fallback when question is outside stored topics
        return "I don't have detailed information regarding that specific topic in my portfolio knowledge base. Feel free to ask about Abhishek's 4+ years of Java/Spring Boot experience, Big Data streaming architecture (~650M records/day), awards at Impetus, or how to email him!";
    }

    private String applyPrivacyGuardrail(String text) {
        if (text == null) return "";
        // Redact any 10-digit or 12-digit Indian phone numbers
        String sanitized = PHONE_GUARDRAIL_PATTERN.matcher(text).replaceAll("[Contact via email: abhimandloi111@gmail.com]");
        return sanitized;
    }
}
