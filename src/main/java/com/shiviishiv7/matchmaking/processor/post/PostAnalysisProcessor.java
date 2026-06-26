package com.shiviishiv7.matchmaking.processor.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.processor.matchingengine.MatchingEngineProcessor;
import com.shiviishiv7.matchmaking.processor.userprofile.*;
import com.shiviishiv7.matchmaking.service.match.MatchConnectService;
import com.shiviishiv7.matchmaking.provider.implementation.UserPostRepository;
import com.shiviishiv7.matchmaking.provider.model.UserPost;
import com.shiviishiv7.matchmaking.provider.vo.MatchCandidateVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchDiscoveryRequestVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchFilterVO;
import com.shiviishiv7.matchmaking.provider.vo.post.*;
import com.shiviishiv7.matchmaking.provider.vo.ws.MatchNotificationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostAnalysisProcessor implements IPostAnalysisProcessor {

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${anthropic.api.model:claude-haiku-4-5-20251001}")
    private String model;

    private static final String USER_QUEUE_MATCHES = "/queue/matches";

    private final UserPostRepository userPostRepository;
    private final ObjectMapper objectMapper;
    private final MatchingEngineProcessor matchingEngineProcessor;
    private final MatchConnectService matchConnectService;
    private final SimpMessagingTemplate messagingTemplate;
    private final IMatrimonialExtProfileProcessor matrimonialExtProfileProcessor;
    private final IDatingExtProfileProcessor datingExtProfileProcessor;
    private final IFitnessExtProfileProcessor fitnessExtProfileProcessor;
    private final IFlatmateExtProfileProcessor flatmateExtProfileProcessor;
    private final IGamingExtProfileProcessor gamingExtProfileProcessor;
    private final IProfessionalExtProfileProcessor professionalExtProfileProcessor;
    private final ITravelExtProfileProcessor travelExtProfileProcessor;

    // ─── analyze ───────────────────────────────────────────────────────────────

    @Override
    public PostAnalyzeResponseVO analyze(String postText) {
        try {
            String responseJson = callClaude(buildAnalyzePrompt(postText));
            return parseAnalyzeResponse(responseJson);
        } catch (MatchmakingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Post analysis failed", e);
            throw new MatchmakingException("AI analysis failed. Please try again.", 500);
        }
    }

    // ─── submit ────────────────────────────────────────────────────────────────

    @Override
    public PostSubmitResponseVO submit(String cognitoSub, PostSubmitRequestVO request) {
        try {
            // Single Claude call: infer category + extract profile fields
            String responseJson = callClaude(buildSubmitPrompt(request.getPostText(), request.getAnswers()));
            JsonNode root = objectMapper.readTree(responseJson);

            MatchCategory category = MatchCategory.fromName(root.path("inferredCategory").asText());

            // Save post
            String answersJson = objectMapper.writeValueAsString(request.getAnswers());
            UserPost post = UserPost.builder()
                    .cognitoSub(cognitoSub)
                    .postText(request.getPostText())
                    .answersJson(answersJson)
                    .inferredCategory(category != null ? category.name() : null)
                    .profileUpdated(false)
                    .build();
            post = userPostRepository.save(post);

            // Enrich profile from extracted fields + trigger matching (async)
            if (category != null && root.has("profile")) {
                MatchFilterVO filterVO = buildFilterVO(cognitoSub, category, root.path("profile"));
                enrichProfileAndDiscover(filterVO, post.getId());
            }

            return PostSubmitResponseVO.builder()
                    .postId(post.getId())
                    .inferredCategory(category)
                    .categoryDisplayName(category != null ? category.getDisplayName() : null)
                    .profileUpdated(category != null)
                    .build();

        } catch (MatchmakingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Post submit failed", e);
            throw new MatchmakingException("Failed to save post. Please try again.", 500);
        }
    }

    // ─── async: upsert profile + run discovery ─────────────────────────────────

    @Async
    protected void enrichProfileAndDiscover(MatchFilterVO filterVO, Long postId) {
        String cognitoSub = filterVO.getCognitoSub();
        try {
            upsertCategoryProfile(filterVO);
            log.info("Profile upserted for cognitoSub={} category={}", cognitoSub, filterVO.getChildCategory());

            userPostRepository.findById(postId).ifPresent(p -> {
                p.setProfileUpdated(true);
                userPostRepository.save(p);
            });

            MatchDiscoveryRequestVO discoveryRequest = new MatchDiscoveryRequestVO();
            discoveryRequest.setCognitoSubA(cognitoSub);
            discoveryRequest.setMatchCategory(MatchCategory.valueOf(filterVO.getChildCategory()));
            discoveryRequest.setPage(0);
            discoveryRequest.setPageSize(20);

            // Run search ONCE — all candidates saved as PENDING MatchResults
            List<MatchCandidateVO> candidates = matchingEngineProcessor.discover(discoveryRequest);
            log.info("Discovery saved {} PENDING matches for cognitoSub={}", candidates.size(), cognitoSub);

            if (candidates.isEmpty()) {
                // No candidates at all — nothing to connect
                messagingTemplate.convertAndSendToUser(cognitoSub, USER_QUEUE_MATCHES,
                        MatchNotificationVO.builder()
                                .event("POST_NO_MATCH_FOUND")
                                .message("No match found yet. We'll notify you when someone matches your profile.")
                                .build());
                log.info("No candidates found for cognitoSub={}", cognitoSub);
                return;
            }

            // Try to connect with the first online candidate right now
            boolean connected = matchConnectService.connectNextOnlineMatch(cognitoSub);

            if (connected) {
                // connectNextOnlineMatch already pushed WAITING_ROOM to both users via /queue/meeting
                // Send a supplementary matches notification so the UI can show context
                messagingTemplate.convertAndSendToUser(cognitoSub, USER_QUEUE_MATCHES,
                        MatchNotificationVO.builder()
                                .event("POST_MATCH_CONNECTING")
                                .message("Match found and connecting now! Check your call screen.")
                                .build());
            } else {
                // Candidates exist but none are online right now
                // Email both sides: user A (saved match), top candidate B (someone is waiting)
                MatchCandidateVO top = candidates.get(0);
                matchConnectService.sendNoOnlineMatchEmails(cognitoSub, top.getCognitoSubB());

                messagingTemplate.convertAndSendToUser(cognitoSub, USER_QUEUE_MATCHES,
                        MatchNotificationVO.builder()
                                .event("POST_NO_ACTIVE_MATCH")
                                .message("Match saved! No one is online right now. We'll notify you when your match comes online.")
                                .build());
                log.info("No online candidate found for cognitoSub={} — emails sent, waiting", cognitoSub);
            }

        } catch (Exception e) {
            log.error("ALERT_FOR_ERROR: Post-submit enrichment failed for postId={}: {}", postId, e.getMessage(), e);
            messagingTemplate.convertAndSendToUser(cognitoSub, USER_QUEUE_MATCHES,
                    MatchNotificationVO.builder()
                            .event("POST_MATCH_ERROR")
                            .message("We saved your post but couldn't run matching right now. Please try again shortly.")
                            .build());
        }
    }

    // ─── route upsert by category group ────────────────────────────────────────

    private void upsertCategoryProfile(MatchFilterVO vo) throws MatchmakingException {
        MatchCategory category = MatchCategory.fromName(vo.getChildCategory());
        if (category == null) return;

        switch (category.getParentGroup()) {
            case "Personal & Lifestyle Connections" -> {
                if (category == MatchCategory.PROFESSIONAL_MATRIMONY) {
                    matrimonialExtProfileProcessor.upsertFromFilter(vo);
                } else if (category == MatchCategory.CASUAL_DATING) {
                    datingExtProfileProcessor.upsertFromFilter(vo);
                }
            }
            case "Health & Wellness", "Shared Activities & Daily Routines" -> {
                if (category == MatchCategory.GYM_PARTNER || category == MatchCategory.YOGA_MEDITATION
                        || category == MatchCategory.CYCLING_RUNNING || category == MatchCategory.FITNESS_SPORTS) {
                    fitnessExtProfileProcessor.upsertFromFilter(vo);
                } else if (category == MatchCategory.GAMING_BUDDIES) {
                    gamingExtProfileProcessor.upsertFromFilter(vo);
                }
            }
            case "Travel & Exploration" -> {
                if (category == MatchCategory.FLATMATE_FINDER) {
                    flatmateExtProfileProcessor.upsertFromFilter(vo);
                } else {
                    travelExtProfileProcessor.upsertFromFilter(vo);
                }
            }
            case "Professional & Career Growth" -> professionalExtProfileProcessor.upsertFromFilter(vo);
            default -> log.warn("No ext profile upsert mapped for category: {}", category);
        }
    }

    // ─── build MatchFilterVO from Claude's extracted profile JSON ───────────────

    private MatchFilterVO buildFilterVO(String cognitoSub, MatchCategory category, JsonNode profileNode) throws Exception {
        // Deserialize extracted fields directly into MatchFilterVO (ignores unknown fields)
        MatchFilterVO vo = objectMapper.treeToValue(profileNode, MatchFilterVO.class);
        vo.setCognitoSub(cognitoSub);
        vo.setChildCategory(category.name());
        vo.setParentCategory(category.getParentGroup());
        return vo;
    }

    // ─── Claude call ───────────────────────────────────────────────────────────

    protected String callClaude(String prompt) throws Exception {
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

        if (response.statusCode() != 200) {
            log.error("Claude API error: status={} body={}", response.statusCode(), response.body());
            throw new MatchmakingException("AI service unavailable.", 502);
        }

        JsonNode root = objectMapper.readTree(response.body());
        String text = root.path("content").get(0).path("text").asText();
        return text.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();
    }

    // ─── prompt: analyze ───────────────────────────────────────────────────────

    private String buildAnalyzePrompt(String postText) {
        String categories = Arrays.stream(MatchCategory.values())
                .map(c -> c.name() + " (" + c.getDisplayName() + ")")
                .collect(Collectors.joining(", "));

        return """
                You are a matchmaking assistant helping users write better connection posts.

                Read the user's draft post below and:
                1. Infer the most suitable match category from this list: %s
                2. Identify 2–4 important pieces of information that are missing or unclear.
                3. For each missing piece, generate a focused follow-up question with the most appropriate UI input type.

                Respond ONLY with valid JSON — no preamble, no markdown. Use exactly this structure:

                {
                  "inferredCategory": "<ENUM_NAME from the list above>",
                  "questions": [
                    {
                      "id": "q1",
                      "question": "<the follow-up question>",
                      "type": "<text|single_choice|multi_choice|range|boolean|dropdown>",
                      "options": ["option1", "option2"],
                      "placeholder": "<hint text, only for type=text>",
                      "min": <number, only for type=range>,
                      "max": <number, only for type=range>
                    }
                  ]
                }

                Rules:
                - Use "single_choice" when the answer is one of a small fixed set (2–5 options).
                - Use "multi_choice" for multiple selections (e.g. languages, interests).
                - Use "range" for numeric ranges like age (min/max required).
                - Use "boolean" for simple yes/no questions (no options needed).
                - Use "dropdown" for large fixed lists (e.g. Indian states, religions).
                - Use "text" for open-ended answers.
                - Only include "options" for single_choice, multi_choice, or dropdown types.
                - Only include "min"/"max" for range type.
                - Only include "placeholder" for text type.
                - Ask only about things genuinely missing from the post.

                USER POST:
                %s
                """.formatted(categories, postText);
    }

    // ─── prompt: submit ────────────────────────────────────────────────────────

    private String buildSubmitPrompt(String postText, List<PostAnswerVO> answers) {
        String categories = Arrays.stream(MatchCategory.values())
                .map(c -> c.name() + " (" + c.getDisplayName() + ")")
                .collect(Collectors.joining(", "));

        String answersText = answers == null ? "None" : answers.stream()
                .map(a -> "- " + a.getQuestionId() + ": " + a.getValue())
                .collect(Collectors.joining("\n"));

        return """
                Based on the matchmaking post and follow-up answers below, do two things:
                1. Determine the best match category from this list: %s
                2. Extract all available profile fields from the post and answers.

                Respond ONLY with valid JSON — no preamble, no markdown:

                {
                  "inferredCategory": "<ENUM_NAME>",
                  "profile": {
                    "religion": "<if mentioned>",
                    "caste": "<if mentioned>",
                    "motherTongue": "<if mentioned>",
                    "dietaryHabits": "<Vegetarian|Non-Vegetarian|Vegan|Jain|Other>",
                    "highestEducation": "<if mentioned>",
                    "profession": "<if mentioned>",
                    "nativeCity": "<if mentioned>",
                    "nativeState": "<if mentioned>",
                    "maritalStatus": "<Never Married|Divorced|Widowed|Awaiting Divorce>",
                    "smokingHabit": "<Non-Smoker|Occasional|Regular>",
                    "drinkingHabit": "<Non-Drinker|Occasional|Regular>",
                    "preferredCity": "<preferred location if mentioned>",
                    "preferredState": "<preferred state if mentioned>",
                    "minAge": <preferred min age as integer, if mentioned>,
                    "maxAge": <preferred max age as integer, if mentioned>,
                    "preferredGender": "<Male|Female|Any>",
                    "relationshipGoal": "<for dating: Long-Term|Casual|Marriage|Open>",
                    "currentRole": "<for professional>",
                    "industryDomain": "<for professional>",
                    "travelStyle": "<for travel>",
                    "fitnessLevel": "<for fitness>",
                    "platforms": "<for gaming>",
                    "lookingIn": "<for flatmate: city/area>"
                  }
                }

                Rules:
                - Only include fields that are clearly present or strongly implied in the post/answers.
                - Omit fields entirely (do not set null/empty) if not mentioned.
                - Do not guess or fabricate values.

                POST:
                %s

                FOLLOW-UP ANSWERS:
                %s
                """.formatted(categories, postText, answersText);
    }

    // ─── response parsing ──────────────────────────────────────────────────────

    private PostAnalyzeResponseVO parseAnalyzeResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        MatchCategory category = MatchCategory.fromName(root.path("inferredCategory").asText());

        List<PostQuestionVO> questions = new ArrayList<>();
        for (JsonNode q : root.path("questions")) {
            PostQuestionVO vo = new PostQuestionVO();
            vo.setId(q.path("id").asText());
            vo.setQuestion(q.path("question").asText());
            vo.setType(q.path("type").asText());
            vo.setPlaceholder(q.has("placeholder") ? q.path("placeholder").asText() : null);
            vo.setMin(q.has("min") ? q.path("min").asInt() : null);
            vo.setMax(q.has("max") ? q.path("max").asInt() : null);

            if (q.has("options") && q.path("options").isArray()) {
                List<String> opts = new ArrayList<>();
                q.path("options").forEach(o -> opts.add(o.asText()));
                vo.setOptions(opts);
            }
            questions.add(vo);
        }

        return PostAnalyzeResponseVO.builder()
                .inferredCategory(category)
                .categoryDisplayName(category != null ? category.getDisplayName() : null)
                .questions(questions)
                .build();
    }
}
