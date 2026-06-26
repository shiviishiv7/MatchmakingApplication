package com.shiviishiv7.matchmaking.service.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.ChatExtraAttributeRepository;
import com.shiviishiv7.matchmaking.provider.implementation.ChatQuestionBankRepository;
import com.shiviishiv7.matchmaking.provider.implementation.ChatSessionRepository;
import com.shiviishiv7.matchmaking.provider.model.ChatExtraAttribute;
import com.shiviishiv7.matchmaking.provider.model.ChatQuestionBank;
import com.shiviishiv7.matchmaking.provider.model.ChatSession;
import com.shiviishiv7.matchmaking.provider.vo.chat.ChatMessageVO;
import com.shiviishiv7.matchmaking.provider.vo.chat.ChatSessionVO;
import com.shiviishiv7.matchmaking.provider.vo.chat.ChatSseEvent;
import com.shiviishiv7.matchmaking.service.category.CategoryCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MIN_QUESTIONS = 10;
    private static final int MAX_QUESTIONS = 25;

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${anthropic.api.model:claude-haiku-4-5-20251001}")
    private String model;

    private final ChatSessionRepository sessionRepository;
    private final ChatQuestionBankRepository questionBankRepository;
    private final ChatExtraAttributeRepository extraAttributeRepository;
    private final CategoryCacheService categoryCacheService;
    private final ObjectMapper objectMapper;

    // ─── start session ─────────────────────────────────────────────────────────

    public ChatSessionVO startSession(String cognitoSub) {
        String openingMessage = "Hey! 👋 What are you looking for today? You can describe it in your own words, or browse the categories below to get started.";

        List<ChatMessageVO> history = new ArrayList<>();
        history.add(new ChatMessageVO("assistant", openingMessage));

        ChatSession session = ChatSession.builder()
                .cognitoSub(cognitoSub)
                .conversationHistory(toJson(history))
                .status("IN_PROGRESS")
                .questionCount(0)
                .build();

        session = sessionRepository.save(session);
        return ChatSessionVO.from(session, history);
    }

    // ─── get session ───────────────────────────────────────────────────────────

    public ChatSessionVO getSession(String cognitoSub, Long sessionId) {
        ChatSession session = findSession(cognitoSub, sessionId);
        List<ChatMessageVO> history = parseHistory(session.getConversationHistory());
        return ChatSessionVO.from(session, history);
    }

    // ─── list sessions ─────────────────────────────────────────────────────────

    public List<ChatSessionVO> listSessions(String cognitoSub) {
        return sessionRepository.findByCognitoSubOrderByCreatedAtDesc(cognitoSub)
                .stream()
                .map(s -> ChatSessionVO.from(s, parseHistory(s.getConversationHistory())))
                .collect(Collectors.toList());
    }

    // ─── send message (SSE streaming) ─────────────────────────────────────────

    public SseEmitter sendMessage(String cognitoSub, Long sessionId, String userContent) {
        SseEmitter emitter = new SseEmitter(120_000L);

        ChatSession session = findSession(cognitoSub, sessionId);

        if ("SUBMITTED".equals(session.getStatus())) {
            throw new MatchmakingException("This session has already been submitted.", 400);
        }

        List<ChatMessageVO> history = parseHistory(session.getConversationHistory());
        history.add(new ChatMessageVO("user", userContent));

        new Thread(() -> {
            try {
                StringBuilder fullResponse = new StringBuilder();

                streamClaude(buildSystemPrompt(session), history, chunk -> {
                    fullResponse.append(chunk);
                    emitter.send(SseEmitter.event()
                            .name("token")
                            .data(chunk));
                });

                // Add bot response to history
                String botMessage = fullResponse.toString();
                history.add(new ChatMessageVO("assistant", botMessage));

                // Extract structured metadata from the conversation
                ExtractedMetadata meta = extractMetadata(session, history, userContent, botMessage);

                // Persist session updates
                session.setConversationHistory(toJson(history));
                session.setQuestionCount(meta.questionCount != null
                        ? meta.questionCount : session.getQuestionCount());
                if (meta.detectedCategory != null) {
                    session.setDetectedCategory(meta.detectedCategory);
                }
                if ("AWAITING_SUBMIT".equals(meta.status)) {
                    session.setStatus("AWAITING_SUBMIT");
                }
                if (meta.collectedAttributes != null) {
                    session.setCollectedAttributes(toJson(meta.collectedAttributes));
                }
                sessionRepository.save(session);

                // Save extra attributes
                if (meta.extraAttributes != null) {
                    saveExtraAttributes(session, meta.extraAttributes);
                }

                // Save new questions to bank
                if (meta.newQuestions != null) {
                    saveToQuestionBank(session.getDetectedCategory(), meta.newQuestions);
                }

                emitter.send(SseEmitter.event()
                        .name("metadata")
                        .data(objectMapper.writeValueAsString(ChatSseEvent.builder()
                                .type("metadata")
                                .detectedCategory(session.getDetectedCategory())
                                .categoryDisplayName(getCategoryDisplayName(session.getDetectedCategory()))
                                .status(session.getStatus())
                                .questionCount(session.getQuestionCount())
                                .detectedCategories(meta.detectedCategories)
                                .build())));

                emitter.send(SseEmitter.event().name("done").data(""));
                emitter.complete();

            } catch (Exception e) {
                log.error("Chat stream error for sessionId={}", sessionId, e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    // ─── submit ────────────────────────────────────────────────────────────────

    public Map<String, Object> submitSession(String cognitoSub, Long sessionId) {
        ChatSession session = findSession(cognitoSub, sessionId);

        if (session.getDetectedCategory() == null) {
            throw new MatchmakingException("No category detected yet. Please continue the conversation.", 400);
        }
        if ("SUBMITTED".equals(session.getStatus())) {
            throw new MatchmakingException("Session already submitted.", 400);
        }

        session.setStatus("SUBMITTED");
        sessionRepository.save(session);

        Map<String, Object> collectedAttributes = session.getCollectedAttributes() != null
                ? fromJson(session.getCollectedAttributes())
                : Map.of();

        return Map.of(
                "sessionId", sessionId,
                "category", session.getDetectedCategory(),
                "collectedAttributes", collectedAttributes
        );
    }

    // ─── Claude streaming call ─────────────────────────────────────────────────

    private void streamClaude(String systemPrompt, List<ChatMessageVO> history,
                              ThrowingConsumer<String> onChunk) throws Exception {

        List<Map<String, String>> messages = history.stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .collect(Collectors.toList());

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", model,
                "max_tokens", 1024,
                "system", systemPrompt,
                "stream", true,
                "messages", messages
        ));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ANTHROPIC_API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<java.io.InputStream> response = client.send(request,
                HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new MatchmakingException("AI service unavailable.", 502);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if (data.equals("[DONE]") || data.isEmpty()) continue;
                    try {
                        JsonNode node = objectMapper.readTree(data);
                        if ("content_block_delta".equals(node.path("type").asText())) {
                            String text = node.path("delta").path("text").asText("");
                            if (!text.isEmpty()) onChunk.accept(text);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    // ─── non-streaming Claude call for metadata extraction ─────────────────────

    private String callClaude(String prompt) throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", model,
                "max_tokens", 1500,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        ));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ANTHROPIC_API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new MatchmakingException("AI service unavailable.", 502);

        JsonNode root = objectMapper.readTree(response.body());
        String text = root.path("content").get(0).path("text").asText();
        return text.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();
    }

    // ─── extract structured metadata after streaming ───────────────────────────

    private ExtractedMetadata extractMetadata(ChatSession session,
                                              List<ChatMessageVO> history,
                                              String userMessage,
                                              String botMessage) {
        try {
            String allCategories = categoryCacheService.getAllCategories().stream()
                    .map(c -> c.getEnumKey() + " (" + c.getDisplayName() + ")")
                    .collect(Collectors.joining(", "));

            String prompt = buildMetadataExtractionPrompt(session, history, userMessage, botMessage, allCategories);
            String json = callClaude(prompt);
            JsonNode root = objectMapper.readTree(json);

            ExtractedMetadata meta = new ExtractedMetadata();

            // Detected categories
            if (root.has("detectedCategories")) {
                List<String> detected = new ArrayList<>();
                root.path("detectedCategories").forEach(n -> detected.add(n.asText()));
                meta.detectedCategories = detected;
            }

            // Confirmed single category
            if (root.has("confirmedCategory") && !root.path("confirmedCategory").asText().isBlank()) {
                meta.detectedCategory = root.path("confirmedCategory").asText();
            }

            // Collected attributes (ExtProfile fields)
            if (root.has("collectedAttributes")) {
                meta.collectedAttributes = objectMapper.convertValue(
                        root.path("collectedAttributes"),
                        new TypeReference<Map<String, Object>>() {});
            }

            // Extra attributes (unmapped)
            if (root.has("extraAttributes") && root.path("extraAttributes").isArray()) {
                meta.extraAttributes = new ArrayList<>();
                root.path("extraAttributes").forEach(n -> {
                    String key = n.path("key").asText();
                    String value = n.path("value").asText();
                    if (!key.isBlank()) meta.extraAttributes.add(Map.of("key", key, "value", value));
                });
            }

            // New questions to save to bank
            if (root.has("newQuestions") && root.path("newQuestions").isArray()) {
                meta.newQuestions = new ArrayList<>();
                root.path("newQuestions").forEach(n -> {
                    String q = n.path("questionText").asText();
                    String attr = n.has("mappedAttribute") ? n.path("mappedAttribute").asText() : null;
                    if (!q.isBlank()) meta.newQuestions.add(Map.of(
                            "questionText", q,
                            "mappedAttribute", attr != null ? attr : ""
                    ));
                });
            }

            // Question count
            meta.questionCount = root.has("questionCount")
                    ? root.path("questionCount").asInt(session.getQuestionCount())
                    : session.getQuestionCount();

            // Status
            meta.status = root.has("status") ? root.path("status").asText() : "IN_PROGRESS";

            return meta;

        } catch (Exception e) {
            log.warn("Metadata extraction failed for sessionId={}: {}", session.getId(), e.getMessage());
            return new ExtractedMetadata();
        }
    }

    // ─── system prompt ─────────────────────────────────────────────────────────

    private String buildSystemPrompt(ChatSession session) {
        String categoryContext = session.getDetectedCategory() != null
                ? "The user has already selected the category: " + session.getDetectedCategory()
                  + ". Focus all questions on this category."
                : "Your first goal is to understand what the user is looking for and identify their match category.";

        int qCount = session.getQuestionCount() != null ? session.getQuestionCount() : 0;
        String questionGuidance;
        if (qCount < MIN_QUESTIONS) {
            questionGuidance = "You have asked " + qCount + " questions so far. Keep asking — minimum is " + MIN_QUESTIONS + ".";
        } else if (qCount >= MAX_QUESTIONS) {
            questionGuidance = "You have reached the maximum of " + MAX_QUESTIONS + " questions. Wrap up and ask the user if they are ready to find their match.";
        } else {
            questionGuidance = "You have asked " + qCount + " questions. After question 10, you may ask the user if they want to continue or submit.";
        }

        return """
                You are a friendly matchmaking assistant helping users find their perfect connection.

                %s

                %s

                Rules:
                - Be warm, conversational, and encouraging.
                - Ask ONE question at a time. Never bombard with multiple questions.
                - If the user says "Hi" or sends a greeting, respond warmly and ask what they are looking for.
                - If multiple categories seem possible, list them and ask the user to pick one.
                - Once a category is confirmed, ask targeted questions to build their profile.
                - Cover: location, preferences, personality, goals, availability, deal-breakers.
                - Keep responses concise and friendly.
                - Do NOT ask for personal sensitive data (phone, address, etc.).
                """.formatted(categoryContext, questionGuidance);
    }

    // ─── metadata extraction prompt ────────────────────────────────────────────

    private String buildMetadataExtractionPrompt(ChatSession session,
                                                  List<ChatMessageVO> history,
                                                  String userMessage,
                                                  String botMessage,
                                                  String allCategories) {
        String currentAttrs = session.getCollectedAttributes() != null
                ? session.getCollectedAttributes() : "{}";

        String historyText = history.stream()
                .map(m -> m.getRole().toUpperCase() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

        return """
                You are a data extraction assistant for a matchmaking app.

                Available categories: %s

                Current collected attributes: %s
                Current question count: %d
                Current detected category: %s

                Latest exchange:
                USER: %s
                BOT: %s

                Full conversation:
                %s

                Extract and return ONLY valid JSON — no markdown, no preamble:

                {
                  "detectedCategories": ["<enumKey1>", "<enumKey2>"],
                  "confirmedCategory": "<single enumKey if user confirmed, else empty string>",
                  "collectedAttributes": {
                    "<fieldName>": "<value extracted from user messages>"
                  },
                  "extraAttributes": [
                    {"key": "<camelCaseKey>", "value": "<value>"}
                  ],
                  "newQuestions": [
                    {"questionText": "<question the bot just asked>", "mappedAttribute": "<field name or empty>"}
                  ],
                  "questionCount": <total questions asked so far as integer>,
                  "status": "<IN_PROGRESS|AWAITING_SUBMIT>"
                }

                Rules:
                - detectedCategories: list all categories mentioned or implied by the user (can be multiple).
                - confirmedCategory: only set if user explicitly picked one category.
                - collectedAttributes: ONLY include fields clearly stated by the user. Merge with current attributes.
                - extraAttributes: attributes the user mentioned that don't map to standard profile fields.
                - newQuestions: any new question the bot asked in this turn (to save to question bank).
                - questionCount: total number of questions the bot has asked across the whole conversation.
                - status: set to AWAITING_SUBMIT only if bot asked "are you ready to find your match?" AND user said yes or confirmed.
                - Omit empty arrays/objects.
                """.formatted(
                allCategories,
                currentAttrs,
                session.getQuestionCount() != null ? session.getQuestionCount() : 0,
                session.getDetectedCategory() != null ? session.getDetectedCategory() : "none",
                userMessage,
                botMessage,
                historyText
        );
    }

    // ─── helpers ───────────────────────────────────────────────────────────────

    private void saveExtraAttributes(ChatSession session, List<Map<String, String>> extras) {
        if (session.getDetectedCategory() == null) return;
        extras.forEach(e -> {
            String key = e.getOrDefault("key", "");
            String value = e.getOrDefault("value", "");
            if (!key.isBlank() && !value.isBlank()) {
                extraAttributeRepository.save(ChatExtraAttribute.builder()
                        .sessionId(session.getId())
                        .cognitoSub(session.getCognitoSub())
                        .matchCategory(session.getDetectedCategory())
                        .attributeKey(key)
                        .attributeValue(value)
                        .build());
            }
        });
    }

    private void saveToQuestionBank(String category, List<Map<String, String>> questions) {
        if (category == null) return;
        questions.forEach(q -> {
            String text = q.getOrDefault("questionText", "");
            String attr = q.getOrDefault("mappedAttribute", null);
            if (!text.isBlank() && !questionBankRepository.existsByMatchCategoryAndQuestionText(category, text)) {
                questionBankRepository.save(ChatQuestionBank.builder()
                        .matchCategory(category)
                        .questionText(text)
                        .mappedAttribute(attr != null && !attr.isBlank() ? attr : null)
                        .isActive(true)
                        .build());
            }
        });
    }

    private String getCategoryDisplayName(String enumKey) {
        if (enumKey == null) return null;
        return categoryCacheService.findByEnumKey(enumKey)
                .map(c -> c.getDisplayName())
                .orElse(enumKey);
    }

    private ChatSession findSession(String cognitoSub, Long sessionId) {
        return sessionRepository.findById(sessionId)
                .filter(s -> s.getCognitoSub().equals(cognitoSub))
                .orElseThrow(() -> new MatchmakingException("Session not found.", 404));
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "[]"; }
    }

    private List<ChatMessageVO> parseHistory(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return new ArrayList<>(); }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String json) {
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return new HashMap<>(); }
    }

    // ─── inner types ───────────────────────────────────────────────────────────

    @FunctionalInterface
    interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    private static class ExtractedMetadata {
        String detectedCategory;
        List<String> detectedCategories;
        Map<String, Object> collectedAttributes;
        List<Map<String, String>> extraAttributes;
        List<Map<String, String>> newQuestions;
        Integer questionCount;
        String status;
    }
}
