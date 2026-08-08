package com.abhishek.portfolio.service;

import com.abhishek.portfolio.model.ResumeChunk;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RagService {

    private static final Logger logger = LoggerFactory.getLogger(RagService.class);
    private final List<ResumeChunk> chunks = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "to", "at", "by", "for", "with", "about",
            "against", "between", "into", "through", "during", "before", "after", "above", "below",
            "from", "up", "down", "in", "out", "on", "off", "over", "under", "again", "further",
            "then", "once", "here", "there", "when", "where", "why", "how", "all", "any", "both",
            "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only",
            "own", "same", "so", "than", "too", "very", "can", "will", "just", "don", "should",
            "now", "what", "which", "who", "whom", "this", "that", "these", "those", "tell", "me"
    );

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("rag-data.json");
            try (InputStream inputStream = resource.getInputStream()) {
                List<ResumeChunk> loaded = objectMapper.readValue(inputStream, new TypeReference<List<ResumeChunk>>() {});
                chunks.addAll(loaded);
                logger.info("Loaded {} resume context chunks for RAG", chunks.size());
            }
        } catch (Exception e) {
            logger.error("Failed to load rag-data.json context data", e);
        }
    }

    public List<ResumeChunk> retrieveContextChunks(String userQuestion, int topK) {
        if (chunks.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> keywords = extractKeywords(userQuestion);
        if (keywords.isEmpty()) {
            return chunks.stream().limit(topK).toList();
        }

        Map<ResumeChunk, Integer> scores = new HashMap<>();

        for (ResumeChunk chunk : chunks) {
            int score = 0;
            String lowerContent = chunk.content().toLowerCase();
            String lowerCategory = chunk.category().toLowerCase();

            for (String kw : keywords) {
                // High weight for matching tags
                if (chunk.tags() != null) {
                    for (String tag : chunk.tags()) {
                        if (tag.toLowerCase().contains(kw)) {
                            score += 5;
                        }
                    }
                }
                // Category match
                if (lowerCategory.contains(kw)) {
                    score += 4;
                }
                // Content match
                if (lowerContent.contains(kw)) {
                    score += 2;
                }
            }

            if (score > 0) {
                scores.put(chunk, score);
            }
        }

        List<ResumeChunk> ranked = scores.entrySet().stream()
                .sorted(Map.Entry.<ResumeChunk, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(topK)
                .collect(Collectors.toList());

        // Fallback to top summary chunks if no specific keywords matched
        if (ranked.isEmpty()) {
            return chunks.stream().limit(topK).toList();
        }

        return ranked;
    }

    public List<ResumeChunk> getAllChunks() {
        return Collections.unmodifiableList(chunks);
    }

    private Set<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }

        String[] tokens = text.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");
        Set<String> result = new HashSet<>();
        for (String token : tokens) {
            if (token.length() > 2 && !STOP_WORDS.contains(token)) {
                result.add(token);
            }
        }
        return result;
    }
}
