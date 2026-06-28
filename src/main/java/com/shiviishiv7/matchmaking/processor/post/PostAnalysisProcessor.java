package com.shiviishiv7.matchmaking.processor.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiviishiv7.matchmaking.common.enums.*;

import java.math.BigDecimal;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.BaseUserProfileRepository;
import com.shiviishiv7.matchmaking.provider.implementation.PartnerPreferenceRepository;
import com.shiviishiv7.matchmaking.provider.implementation.UserPostRepository;
import com.shiviishiv7.matchmaking.provider.model.PartnerPreference;
import com.shiviishiv7.matchmaking.provider.model.UserPost;
import com.shiviishiv7.matchmaking.provider.model.profile.BaseUserProfile;
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
import java.util.Optional;
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

    private final BaseUserProfileRepository baseUserProfileRepository;
    private final UserPostRepository userPostRepository;
    private final PartnerPreferenceRepository partnerPreferenceRepository;
    private final ObjectMapper objectMapper;

    // ─── analyze ───────────────────────────────────────────────────────────────

    @Override
    public PostAnalyzeResponseVO analyze(String postText, IntentType intent, String sub) {
        try {

            Optional<BaseUserProfile> byCognitoSub = baseUserProfileRepository.findByCognitoSub(sub);


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

            // Build partner preferences from p* answers
            PartnerPreference pref = buildPartnerPreference(post.getId(), cognitoSub, request.getIntent(), request.getAnswers());
            if (pref != null) {
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
                "max_tokens", 4096,
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
              {"id":"d2","label":"Your age","type":"range","min":18,"max":60},
              {"id":"d3","label":"Preferred age range of partner","type":"range","min":18,"max":60},
              {"id":"d4","label":"Preferred gender of partner","type":"single_choice","options":["Male","Female","Any"]},
              {"id":"d5","label":"Current city","type":"city","placeholder":"e.g. Mumbai"},
              {"id":"d6","label":"Diet preference","type":"dropdown","options":["Vegetarian","Non-Vegetarian","Vegan","Jain","No preference"]},
              {"id":"d7","label":"Smoking habit","type":"single_choice","options":["Non-Smoker","Occasional","Regular"]},
              {"id":"d8","label":"Drinking habit","type":"single_choice","options":["Non-Drinker","Occasional","Regular"]},
              {"id":"d9","label":"Willing to relocate for partner?","type":"boolean"},
              {"id":"d10","label":"Religious practice level","type":"single_choice","options":["Very religious","Moderate","Non-religious"]},
              {"id":"d11","label":"Partner working preference","type":"single_choice","options":["Working","Non-working","No preference"]},
            ]
            """;

    private static final String MATRIMONIAL_FIXED_QUESTIONS = """
            [
              {"id":"m1","pairGroup":"g1","label":"Your age","type":"range","min":18,"max":25},
              {"id":"p1","pairGroup":"g1","label":"Partner's age range","type":"range","min":18,"max":25},
              {"id":"m2","pairGroup":"g2","label":"Your height (cm)","type":"range","min":140,"max":210},
              {"id":"p2","pairGroup":"g2","label":"Partner's height range (cm)","type":"range","min":140,"max":210},
              {"id":"m3","pairGroup":"g3","label":"Your current city","type":"city","placeholder":"e.g. Delhi"},
              {"id":"p3","pairGroup":"g3","label":"Partner's preferred city","type":"city","placeholder":"e.g. Mumbai"},
              {"id":"m4","pairGroup":"g4","label":"Your mother tongue","type":"dropdown","options":["Hindi","Tamil","Telugu","Kannada","Malayalam","Bengali","Marathi","Gujarati","Punjabi","Odia","Other"]},
              {"id":"p4","pairGroup":"g4","label":"Partner's mother tongue preference","type":"dropdown","options":["Hindi","Tamil","Telugu","Kannada","Malayalam","Bengali","Marathi","Gujarati","Punjabi","Odia","No preference"]},
              {"id":"m5","pairGroup":"g5","label":"Your religion","type":"dropdown","options":["Hindu","Muslim","Christian","Sikh","Jain","Buddhist","Other"]},
              {"id":"p5","pairGroup":"g5","label":"Partner's religion preference","type":"dropdown","options":["Hindu","Muslim","Christian","Sikh","Jain","Buddhist","No preference"]},
              {"id":"m6","pairGroup":"g6","label":"Your highest qualification","type":"dropdown","options":["High School","Graduate","Post Graduate","PhD","Other"]},
              {"id":"p6","pairGroup":"g6","label":"Partner's minimum qualification","type":"dropdown","options":["High School","Graduate","Post Graduate","PhD","No preference"]},
              {"id":"m7","pairGroup":"g7","label":"Your field of study","type":"dropdown","options":["Engineering / Technology","Medicine / Healthcare","Commerce / Accounting","Arts / Humanities","Law","Science","Management / MBA","Other"]},
              {"id":"p7","pairGroup":"g7","label":"Partner's field of study preference","type":"dropdown","options":["Engineering / Technology","Medicine / Healthcare","Commerce / Accounting","Arts / Humanities","Law","Science","Management / MBA","No preference"]},
              {"id":"m8","pairGroup":"g8","label":"Your current profession","type":"dropdown","options":["Software Engineer / IT","Doctor / Healthcare","Teacher / Professor","Lawyer","CA / Finance","Business Owner","Government / Civil Services","Other"]},
              {"id":"p8","pairGroup":"g8","label":"Partner's profession preference","type":"dropdown","options":["Software Engineer / IT","Doctor / Healthcare","Teacher / Professor","Lawyer","CA / Finance","Business Owner","Government / Civil Services","No preference"]},
              {"id":"m9","pairGroup":"g9","label":"Your annual income","type":"dropdown","options":["Below 5L","5–10L","10–20L","20–50L","50L+"]},
              {"id":"p9","pairGroup":"g9","label":"Partner's minimum income expectation","type":"dropdown","options":["Below 5L","5–10L","10–20L","20–50L","No preference"]},
              {"id":"m10","pairGroup":"g10","label":"Your working status","type":"single_choice","options":["Employed","Business Owner","Not Working"]},
              {"id":"p10","pairGroup":"g10","label":"OK if partner is not working?","type":"boolean"},
              {"id":"m11","pairGroup":"g11","label":"Your family type","type":"single_choice","options":["Nuclear","Joint"]},
              {"id":"p11","pairGroup":"g11","label":"Partner's preferred family type","type":"single_choice","options":["Nuclear","Joint","No preference"]},
              {"id":"m12","pairGroup":"g12","label":"Your family values","type":"single_choice","options":["Orthodox","Moderate","Liberal"]},
              {"id":"p12","pairGroup":"g12","label":"Partner's family values preference","type":"single_choice","options":["Orthodox","Moderate","Liberal","No preference"]},
              {"id":"m13","pairGroup":"g13","label":"Your diet preference","type":"dropdown","options":["Vegetarian","Non-Vegetarian","Jain","Vegan"]},
              {"id":"p13","pairGroup":"g13","label":"Partner's diet preference","type":"dropdown","options":["Vegetarian","Non-Vegetarian","Jain","Vegan","No preference"]},
              {"id":"m14","pairGroup":"g14","label":"Your smoking habit","type":"single_choice","options":["Non-Smoker","Occasional","Regular"]},
              {"id":"p14","pairGroup":"g14","label":"Partner's smoking preference","type":"single_choice","options":["Non-Smoker","Occasional","Regular","No preference"]},
              {"id":"m15","pairGroup":"g15","label":"Your drinking habit","type":"single_choice","options":["Non-Drinker","Occasional","Regular"]},
              {"id":"p15","pairGroup":"g15","label":"Partner's drinking preference","type":"single_choice","options":["Non-Drinker","Occasional","Regular","No preference"]},
              {"id":"m16","pairGroup":"g16","label":"Your marital status","type":"dropdown","options":["Never Married","Divorced","Widowed"]},
              {"id":"p16","pairGroup":"g16","label":"Partner's marital status preference","type":"dropdown","options":["Never Married","Divorced","Widowed","No preference"]},
              {"id":"m17","label":"When are you looking to get married?","type":"single_choice","options":["Immediately","Within 6 months","Within 1 year","No rush"]},
              {"id":"m18","label":"Type of marriage you prefer","type":"single_choice","options":["Arranged","Love-Arranged"]},
              {"id":"m20","label":"Living preference after marriage","type":"single_choice","options":["Joint family","Independent","Flexible"]},
              {"id":"m22","label":"Willing to relocate after marriage?","type":"boolean"}
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

                STRICT RULES — follow exactly:
                1. Return ONLY unanswered questions from the fixed list above. Never invent new questions.
                2. If all are answered, return an empty questions array.
                3. Preserve EXACTLY the original id, pairGroup, label, type, options, min, max from the list.
                4. Rename "label" to "question" in your output.
                5. NEVER change a question's type — if the list says "dropdown", output "dropdown".
                6. NEVER output type="text" — the only allowed text-like type is "city" (for city/place fields).
                7. ALL non-city attributes use structured types: single_choice, dropdown, range, boolean.
                8. For type="city" include the placeholder. For type="range" include min and max.
                   For type="single_choice" or "dropdown" include the options array exactly as given.
                9. Do NOT include options for boolean or range types.
                10. If a question has a pairGroup, include it. If it has no pairGroup, omit the field.

                Respond ONLY with valid JSON — no preamble, no markdown:

                {
                  "questions": [
                    {
                      "id": "<from fixed list>",
                      "pairGroup": "<from fixed list, omit if absent>",
                      "question": "<label from fixed list>",
                      "type": "<from fixed list — single_choice|multi_choice|dropdown|range|boolean|city>",
                      "options": ["only for single_choice or dropdown"],
                      "placeholder": "only for city type",
                      "min": "only for range",
                      "max": "only for range"
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

        // Maintain insertion order; questions with same pairGroup share a PostQuestionPairVO
        Map<String, PostQuestionPairVO> pairMap = new java.util.LinkedHashMap<>();
        List<PostQuestionPairVO> pairs = new ArrayList<>();

        for (JsonNode q : root.path("questions")) {
            PostQuestionVO vo = new PostQuestionVO();
            vo.setId(q.path("id").asText());
            vo.setPairGroup(q.has("pairGroup") ? q.path("pairGroup").asText() : null);
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

            String pairGroup = vo.getPairGroup();
            if (pairGroup == null || pairGroup.isBlank()) {
                // Unpaired question renders full-width on the left side
                pairs.add(PostQuestionPairVO.builder().aboutYou(vo).build());
            } else if (!pairMap.containsKey(pairGroup)) {
                PostQuestionPairVO pair = new PostQuestionPairVO();
                if (vo.getId().startsWith("p")) {
                    pair.setPartnerPref(vo);
                } else {
                    pair.setAboutYou(vo);
                }
                pairMap.put(pairGroup, pair);
                pairs.add(pair);
            } else {
                PostQuestionPairVO pair = pairMap.get(pairGroup);
                if (vo.getId().startsWith("p")) {
                    pair.setPartnerPref(vo);
                } else {
                    pair.setAboutYou(vo);
                }
            }
        }

        return PostAnalyzeResponseVO.builder()
                .inferredCategory(null)
                .categoryDisplayName(intent.name())
                .questions(pairs)
                .build();
    }

    // ─── Partner preference builder ────────────────────────────────────────────

    private PartnerPreference buildPartnerPreference(Long postId, String cognitoSub,
                                                      IntentType intent, List<PostAnswerVO> answers) {
        if (answers == null || answers.isEmpty()) return null;

        Map<String, String> p = answers.stream()
                .filter(a -> a.getQuestionId() != null && a.getQuestionId().startsWith("p"))
                .collect(Collectors.toMap(PostAnswerVO::getQuestionId, PostAnswerVO::getValue,
                        (a, b) -> a)); // keep first on duplicate

        if (p.isEmpty()) return null;

        PartnerPreference pref = PartnerPreference.builder()
                .postId(postId)
                .cognitoSub(cognitoSub)
                .intent(intent)
                .openToRelocation(false)
                .build();

        parseRange(p.get("p1"), pref::setAgeMin, pref::setAgeMax);
        parseRange(p.get("p2"), pref::setHeightMinCm, pref::setHeightMaxCm);
        pref.setPreferredStates(blank(p.get("p3")));
        pref.setMotherTonguePref(mapLanguage(p.get("p4")));
        pref.setReligionPref(mapReligion(p.get("p5")));
        pref.setEducationPref(mapQualification(p.get("p6")));
        // p7 = field of study pref — no dedicated column, skipped
        pref.setEmploymentTypePref(mapProfession(p.get("p8")));
        mapIncome(p.get("p9"), pref);
        pref.setOkWithPartnerWorkingPref(parseBoolean(p.get("p10")));
        pref.setFamilyTypePref(mapFamilyType(p.get("p11")));
        pref.setFamilyValuesPref(mapFamilyValues(p.get("p12")));
        pref.setDietaryPref(mapDiet(p.get("p13")));
        pref.setSmokingPref(mapSmokingHabit(p.get("p14")));
        pref.setDrinkingPref(mapDrinkingHabit(p.get("p15")));
        pref.setMaritalStatusPref(mapMaritalStatus(p.get("p16")));

        return pref;
    }

    private void parseRange(String value,
                             java.util.function.Consumer<Integer> minSetter,
                             java.util.function.Consumer<Integer> maxSetter) {
        if (value == null || value.isBlank()) return;
        String[] parts = value.split("-");
        if (parts.length == 2) {
            try {
                minSetter.accept(Integer.parseInt(parts[0].trim()));
                maxSetter.accept(Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException ignored) {}
        }
    }

    private Boolean parseBoolean(String v) {
        if (v == null) return null;
        return "true".equalsIgnoreCase(v.trim());
    }

    private String blank(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private Language mapLanguage(String v) {
        if (v == null || v.isBlank() || "No preference".equalsIgnoreCase(v)) return null;
        try { return Language.valueOf(v.trim().toUpperCase()); } catch (Exception e) { return null; }
    }

    private Religion mapReligion(String v) {
        if (v == null || v.isBlank() || "No preference".equalsIgnoreCase(v)) return null;
        return switch (v.trim()) {
            case "Hindu"     -> Religion.HINDUISM;
            case "Muslim"    -> Religion.ISLAM;
            case "Christian" -> Religion.CHRISTIANITY;
            case "Sikh"      -> Religion.SIKHISM;
            case "Buddhist"  -> Religion.BUDDHISM;
            default          -> Religion.OTHER;
        };
    }

    private Qualification mapQualification(String v) {
        if (v == null || v.isBlank() || "No preference".equalsIgnoreCase(v)) return null;
        return switch (v.trim()) {
            case "High School"   -> Qualification.HIGH_SCHOOL;
            case "Graduate"      -> Qualification.GRADUATE;
            case "Post Graduate" -> Qualification.POST_GRADUATE;
            case "PhD"           -> Qualification.PHD;
            default              -> Qualification.OTHER;
        };
    }

    private Profession mapProfession(String v) {
        if (v == null || v.isBlank() || "No preference".equalsIgnoreCase(v)) return null;
        return switch (v.trim()) {
            case "Software Engineer / IT"      -> Profession.SOFTWARE_ENGINEER_IT;
            case "Doctor / Healthcare"         -> Profession.DOCTOR_HEALTHCARE;
            case "Teacher / Professor"         -> Profession.TEACHER_PROFESSOR;
            case "Lawyer"                      -> Profession.LAWYER;
            case "CA / Finance"                -> Profession.CA_FINANCE;
            case "Business Owner"              -> Profession.BUSINESS_OWNER;
            case "Government / Civil Services" -> Profession.GOVERNMENT_CIVIL_SERVICES;
            default                            -> Profession.OTHER;
        };
    }

    private void mapIncome(String v, PartnerPreference pref) {
        if (v == null || v.isBlank() || "No preference".equalsIgnoreCase(v)) return;
        BigDecimal lakh = BigDecimal.valueOf(100_000);
        switch (v.trim()) {
            case "Below 5L" -> pref.setIncomeMaxInr(BigDecimal.valueOf(5).multiply(lakh));
            case "5–10L"  -> { pref.setIncomeMinInr(BigDecimal.valueOf(5).multiply(lakh));  pref.setIncomeMaxInr(BigDecimal.valueOf(10).multiply(lakh)); }
            case "10–20L" -> { pref.setIncomeMinInr(BigDecimal.valueOf(10).multiply(lakh)); pref.setIncomeMaxInr(BigDecimal.valueOf(20).multiply(lakh)); }
            case "20–50L" -> { pref.setIncomeMinInr(BigDecimal.valueOf(20).multiply(lakh)); pref.setIncomeMaxInr(BigDecimal.valueOf(50).multiply(lakh)); }
            case "50L+"     -> pref.setIncomeMinInr(BigDecimal.valueOf(50).multiply(lakh));
        }
    }

    private FamilyType mapFamilyType(String v) {
        if (v == null || v.isBlank() || "No preference".equalsIgnoreCase(v)) return null;
        return switch (v.trim()) {
            case "Nuclear" -> FamilyType.NUCLEAR;
            case "Joint"   -> FamilyType.JOINT;
            default        -> null;
        };
    }

    private FamilyValues mapFamilyValues(String v) {
        if (v == null || v.isBlank() || "No preference".equalsIgnoreCase(v)) return null;
        return switch (v.trim()) {
            case "Orthodox" -> FamilyValues.ORTHODOX;
            case "Moderate" -> FamilyValues.MODERATE;
            case "Liberal"  -> FamilyValues.LIBERAL;
            default         -> null;
        };
    }

    private DietPreference mapDiet(String v) {
        if (v == null || v.isBlank() || "No preference".equalsIgnoreCase(v)) return null;
        return switch (v.trim()) {
            case "Vegetarian"     -> DietPreference.VEGETARIAN;
            case "Vegan"          -> DietPreference.VEGAN;
            case "Jain"           -> DietPreference.JAIN;
            case "Non-Vegetarian" -> DietPreference.NONE;
            default               -> null;
        };
    }

    private MaritalStatus mapMaritalStatus(String v) {
        if (v == null || v.isBlank() || "No preference".equalsIgnoreCase(v)) return null;
        return switch (v.trim()) {
            case "Never Married" -> MaritalStatus.SINGLE;
            case "Divorced"      -> MaritalStatus.DIVORCED;
            case "Widowed"       -> MaritalStatus.WIDOWED;
            default              -> null;
        };
    }

    private SmokingHabit mapSmokingHabit(String v) {
        if (v == null || v.isBlank() || "No preference".equalsIgnoreCase(v)) return null;
        return switch (v.trim()) {
            case "Non-Smoker" -> SmokingHabit.NON_SMOKER;
            case "Occasional" -> SmokingHabit.OCCASIONAL;
            case "Regular"    -> SmokingHabit.REGULAR;
            default           -> null;
        };
    }

    private DrinkingHabit mapDrinkingHabit(String v) {
        if (v == null || v.isBlank() || "No preference".equalsIgnoreCase(v)) return null;
        return switch (v.trim()) {
            case "Non-Drinker" -> DrinkingHabit.NON_DRINKER;
            case "Occasional"  -> DrinkingHabit.OCCASIONAL;
            case "Regular"     -> DrinkingHabit.REGULAR;
            default            -> null;
        };
    }
}
