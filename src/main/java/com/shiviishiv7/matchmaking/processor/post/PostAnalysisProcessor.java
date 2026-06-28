package com.shiviishiv7.matchmaking.processor.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiviishiv7.matchmaking.common.enums.IntentType;
import com.shiviishiv7.matchmaking.common.enums.PostStatus;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.PartnerPreferenceRepository;
import com.shiviishiv7.matchmaking.provider.implementation.UserPostRepository;
import com.shiviishiv7.matchmaking.provider.model.PartnerPreference;
import com.shiviishiv7.matchmaking.provider.model.UserPost;
import com.shiviishiv7.matchmaking.provider.vo.post.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostAnalysisProcessor implements IPostAnalysisProcessor {

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION  = "2023-06-01";
    private static final int POST_EXPIRY_DAYS      = 30;

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${anthropic.api.model:claude-haiku-4-5-20251001}")
    private String model;

    private final UserPostRepository userPostRepository;
    private final PartnerPreferenceRepository partnerPreferenceRepository;
    private final ObjectMapper objectMapper;

    // ─── analyze ───────────────────────────────────────────────────────────────

    @Override
    public PostAnalyzeResponseVO analyze(String postText, IntentType intent) {
        try {
            String responseJson = callClaude(buildAnalyzePrompt(postText, intent));
            return parseAnalyzeResponse(responseJson, intent);
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
            String answersJson = objectMapper.writeValueAsString(request.getAnswers());

            UserPost post = UserPost.builder()
                    .cognitoSub(cognitoSub)
                    .intent(request.getIntent())
                    .postText(request.getPostText())
                    .answersJson(answersJson)
                    .inferredCategory(request.getIntent().name())
                    .status(PostStatus.ACTIVE)
                    .matchCount(0)
                    .expiresAt(LocalDateTime.now().plusDays(POST_EXPIRY_DAYS))
                    .profileUpdated(false)
                    .build();
            post = userPostRepository.save(post);

            // Save partner preferences linked to this post
            if (request.getPartnerPreference() != null) {
                PartnerPreferenceRequestVO prefReq = request.getPartnerPreference();
                PartnerPreference pref = PartnerPreference.builder()
                        .postId(post.getId())
                        .cognitoSub(cognitoSub)
                        .intent(request.getIntent())
                        .ageMin(prefReq.getAgeMin())
                        .ageMax(prefReq.getAgeMax())
                        .heightMinCm(prefReq.getHeightMinCm())
                        .heightMaxCm(prefReq.getHeightMaxCm())
                        .genderPref(prefReq.getGenderPref())
                        .maritalStatusPref(prefReq.getMaritalStatusPref())
                        .preferredStates(prefReq.getPreferredStates())
                        .openToRelocation(prefReq.getOpenToRelocation() != null ? prefReq.getOpenToRelocation() : false)
                        .religionPref(prefReq.getReligionPref())
                        .motherTonguePref(prefReq.getMotherTonguePref())
                        .dietaryPref(prefReq.getDietaryPref())
                        .educationPref(prefReq.getEducationPref())
                        .employmentTypePref(prefReq.getEmploymentTypePref())
                        .incomeMinInr(prefReq.getIncomeMinInr())
                        .incomeMaxInr(prefReq.getIncomeMaxInr())
                        .smokingPref(prefReq.getSmokingPref())
                        .drinkingPref(prefReq.getDrinkingPref())
                        .familyTypePref(prefReq.getFamilyTypePref())
                        .familyValuesPref(prefReq.getFamilyValuesPref())
                        .wantsChildrenPref(prefReq.getWantsChildrenPref())
                        .marriageTimelinePref(prefReq.getMarriageTimelinePref())
                        .okWithPartnerWorkingPref(prefReq.getOkWithPartnerWorkingPref())
                        .relationshipGoalPref(prefReq.getRelationshipGoalPref())
                        .aboutPartner(prefReq.getAboutPartner())
                        .build();
                partnerPreferenceRepository.save(pref);
            }

            log.info("Post {} submitted by {} (intent={}) — queued for matching",
                    post.getId(), cognitoSub, request.getIntent());

            return PostSubmitResponseVO.builder()
                    .postId(post.getId())
                    .inferredCategory(null)
                    .categoryDisplayName(request.getIntent().name())
                    .profileUpdated(false)
                    .build();

        } catch (MatchmakingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Post submit failed", e);
            throw new MatchmakingException("Failed to save post. Please try again.", 500);
        }
    }

    // ─── close post ────────────────────────────────────────────────────────────

    public void closePost(Long postId, String cognitoSub) {
        UserPost post = userPostRepository.findById(postId)
                .orElseThrow(() -> new MatchmakingException("Post not found", 404));
        if (!post.getCognitoSub().equals(cognitoSub)) {
            throw new MatchmakingException("Not authorised to close this post", 403);
        }
        post.setStatus(PostStatus.CLOSED);
        userPostRepository.save(post);
    }

    // ─── Claude call ───────────────────────────────────────────────────────────

    protected String callClaude(String prompt) throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", model,
                "max_tokens", 2000,
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

    // ─── fixed question banks ──────────────────────────────────────────────────

    private static final String DATING_FIXED_QUESTIONS = """
            [
              {"id":"d1","label":"Relationship goal","type":"single_choice","options":["Serious relationship","Casual dating","Open to both"]},
              {"id":"d2","label":"Your age","type":"text","placeholder":"e.g. 27"},
              {"id":"d3","label":"Preferred age range of partner","type":"range","min":18,"max":60},
              {"id":"d4","label":"Preferred gender of partner","type":"single_choice","options":["Male","Female","Any"]},
              {"id":"d5","label":"Current city","type":"text","placeholder":"e.g. Mumbai"},
              {"id":"d6","label":"Diet preference","type":"dropdown","options":["Vegetarian","Non-Vegetarian","Vegan","Jain","No preference"]},
              {"id":"d7","label":"Smoking habit","type":"single_choice","options":["Non-Smoker","Occasional","Regular"]},
              {"id":"d8","label":"Drinking habit","type":"single_choice","options":["Non-Drinker","Occasional","Regular"]},
              {"id":"d9","label":"Willing to relocate for partner?","type":"boolean"},
              {"id":"d10","label":"Religious practice level","type":"single_choice","options":["Very religious","Moderate","Non-religious"]},
              {"id":"d11","label":"Partner working preference","type":"single_choice","options":["Working","Non-working","No preference"]},
              {"id":"d12","label":"Want children?","type":"single_choice","options":["Yes","No","Already have children","Open"]}
            ]
            """;

    private static final String MATRIMONIAL_FIXED_QUESTIONS = """
            [
              {"id":"m1","label":"Your age","type":"text","placeholder":"e.g. 28"},
              {"id":"m2","label":"Height (cm)","type":"range","min":140,"max":220},
              {"id":"m3","label":"Current city","type":"text","placeholder":"e.g. Delhi"},
              {"id":"m4","label":"Native city / native place","type":"text","placeholder":"e.g. Jaipur"},
              {"id":"m5","label":"Mother tongue","type":"dropdown","options":["Hindi","Tamil","Telugu","Kannada","Malayalam","Bengali","Marathi","Gujarati","Punjabi","Odia","Other"]},
              {"id":"m6","label":"Religion","type":"dropdown","options":["Hindu","Muslim","Christian","Sikh","Jain","Buddhist","Other"]},
              {"id":"m7","label":"Highest qualification","type":"dropdown","options":["High School","Graduate","Post Graduate","PhD","Other"]},
              {"id":"m8","label":"Field of study","type":"text","placeholder":"e.g. Computer Science"},
              {"id":"m9","label":"Current profession","type":"text","placeholder":"e.g. Software Engineer"},
              {"id":"m10","label":"Sector","type":"dropdown","options":["IT","Government","Business / Self-employed","Healthcare","Education","Finance","Other"]},
              {"id":"m11","label":"Annual income","type":"dropdown","options":["Below 5L","5–10L","10–20L","20–50L","50L+"]},
              {"id":"m12","label":"Working status","type":"single_choice","options":["Employed","Business Owner","Not Working"]},
              {"id":"m13","label":"Family type","type":"single_choice","options":["Nuclear","Joint"]},
              {"id":"m14","label":"Family values","type":"single_choice","options":["Orthodox","Moderate","Liberal"]},
              {"id":"m15","label":"Father's occupation","type":"text","placeholder":"e.g. Retired government officer"},
              {"id":"m16","label":"Mother's occupation","type":"text","placeholder":"e.g. Homemaker"},
              {"id":"m17","label":"Number of siblings","type":"range","min":0,"max":8},
              {"id":"m18","label":"Diet preference","type":"dropdown","options":["Vegetarian","Non-Vegetarian","Jain","Vegan"]},
              {"id":"m19","label":"Smoking habit","type":"single_choice","options":["Non-Smoker","Occasional","Regular"]},
              {"id":"m20","label":"Drinking habit","type":"single_choice","options":["Non-Drinker","Occasional","Regular"]},
              {"id":"m21","label":"Marital status","type":"dropdown","options":["Never Married","Divorced","Widowed"]},
              {"id":"m22","label":"Do you have children?","type":"boolean"},
              {"id":"m23","label":"When are you looking to get married?","type":"single_choice","options":["Immediately","Within 6 months","Within 1 year","No rush"]},
              {"id":"m24","label":"Type of marriage","type":"single_choice","options":["Arranged","Love-Arranged"]},
              {"id":"m25","label":"Want children?","type":"single_choice","options":["Yes","No","Open"]},
              {"id":"m26","label":"Living preference after marriage","type":"single_choice","options":["Joint family","Independent","Flexible"]},
              {"id":"m27","label":"OK with partner working after marriage?","type":"boolean"},
              {"id":"m28","label":"Willing to relocate after marriage?","type":"boolean"}
            ]
            """;

    // ─── prompt: analyze ───────────────────────────────────────────────────────

    private String buildAnalyzePrompt(String postText, IntentType intent) {
        String fixedQuestions = intent == IntentType.DATING ? DATING_FIXED_QUESTIONS : MATRIMONIAL_FIXED_QUESTIONS;
        String intentLabel = intent == IntentType.DATING ? "Dating" : "Matrimonial";

        return """
                You are a matchmaking assistant for the "%s" category.

                The user has written a post. Read it carefully and extract which attributes from
                the FIXED QUESTION LIST below are already clearly answered in the post.
                Then return ONLY the questions that are NOT answered.

                FIXED QUESTION LIST:
                %s

                Rules:
                - Return ONLY unanswered questions from the list above — never invent new questions.
                - If all are answered, return an empty questions array.
                - Preserve the original id, label, type, options, min, max from the list.
                - Rename "label" to "question" in your output.

                Respond ONLY with valid JSON — no preamble, no markdown:

                {
                  "questions": [
                    {
                      "id": "<from fixed list>",
                      "question": "<label from fixed list>",
                      "type": "<from fixed list>",
                      "options": ["..."],
                      "placeholder": "<only for type=text>",
                      "min": <only for type=range>,
                      "max": <only for type=range>
                    }
                  ]
                }

                USER POST:
                %s
                """.formatted(intentLabel, fixedQuestions, postText);
    }

    // ─── response parsing ──────────────────────────────────────────────────────

    private PostAnalyzeResponseVO parseAnalyzeResponse(String json, IntentType intent) throws Exception {
        JsonNode root = objectMapper.readTree(json);

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
                .inferredCategory(null)
                .categoryDisplayName(intent.name())
                .questions(questions)
                .build();
    }
}
