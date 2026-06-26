package com.shiviishiv7.matchmaking.processor.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.processor.matchingengine.MatchingEngineProcessor;
import com.shiviishiv7.matchmaking.processor.post.PostAnalysisProcessor;
import com.shiviishiv7.matchmaking.processor.userprofile.*;
import com.shiviishiv7.matchmaking.provider.implementation.UserPostRepository;
import com.shiviishiv7.matchmaking.provider.model.UserPost;
import com.shiviishiv7.matchmaking.provider.vo.MatchCandidateVO;
import com.shiviishiv7.matchmaking.provider.vo.ws.MatchNotificationVO;
import com.shiviishiv7.matchmaking.provider.vo.post.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PostAnalysisProcessor.
 *
 * callClaude() is stubbed via spy — no real HTTP calls made.
 */
@ExtendWith(MockitoExtension.class)
class PostAnalysisProcessorTest {

    @Mock private UserPostRepository postRepo;
    @Mock private MatchingEngineProcessor matchingEngine;
    @Mock private com.shiviishiv7.matchmaking.service.match.MatchConnectService matchConnectService;
    @Mock private SimpMessagingTemplate messaging;
    @Mock private IMatrimonialExtProfileProcessor matrimonialProcessor;
    @Mock private IDatingExtProfileProcessor datingProcessor;
    @Mock private IFitnessExtProfileProcessor fitnessProcessor;
    @Mock private IFlatmateExtProfileProcessor flatmateProcessor;
    @Mock private IGamingExtProfileProcessor gamingProcessor;
    @Mock private IProfessionalExtProfileProcessor professionalProcessor;
    @Mock private ITravelExtProfileProcessor travelProcessor;

    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    private PostAnalysisProcessor processor;

    private static final String COGNITO_SUB = "user-123";
    private static final String MATRIMONIAL_POST =
            "27M | Delhi | Looking for a life partner who values sincerity. I am a practicing Muslim working in tech.";

    @BeforeEach
    void setUp() {
        processor = new PostAnalysisProcessor(
                postRepo, objectMapper, matchingEngine, matchConnectService, messaging,
                matrimonialProcessor, datingProcessor, fitnessProcessor,
                flatmateProcessor, gamingProcessor, professionalProcessor, travelProcessor
        );
        ReflectionTestUtils.setField(processor, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(processor, "model", "claude-haiku-4-5-20251001");
    }

    // ─── analyze() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("analyze()")
    class AnalyzeTests {

        @Test
        @DisplayName("Returns inferred category and questions from Claude response")
        void analyze_validPost_returnsQuestionsAndCategory() throws Exception {
            String claudeJson = """
                    {
                      "inferredCategory": "PROFESSIONAL_MATRIMONY",
                      "questions": [
                        {
                          "id": "q1",
                          "question": "Which city are you based in?",
                          "type": "text",
                          "placeholder": "e.g. Delhi, Mumbai"
                        },
                        {
                          "id": "q2",
                          "question": "Are you open to relocating after marriage?",
                          "type": "single_choice",
                          "options": ["Yes", "No", "Open to discussion"]
                        },
                        {
                          "id": "q3",
                          "question": "What age range are you looking for?",
                          "type": "range",
                          "min": 22,
                          "max": 35
                        }
                      ]
                    }
                    """;

            PostAnalysisProcessor spy = spyWithClaude(claudeJson);
            PostAnalyzeResponseVO result = spy.analyze(MATRIMONIAL_POST);

            assertThat(result.getInferredCategory()).isEqualTo(MatchCategory.PROFESSIONAL_MATRIMONY);
            assertThat(result.getCategoryDisplayName()).isEqualTo("Matrimonial (Verified)");
            assertThat(result.getQuestions()).hasSize(3);

            PostQuestionVO q1 = result.getQuestions().get(0);
            assertThat(q1.getId()).isEqualTo("q1");
            assertThat(q1.getType()).isEqualTo("text");
            assertThat(q1.getPlaceholder()).isEqualTo("e.g. Delhi, Mumbai");

            PostQuestionVO q2 = result.getQuestions().get(1);
            assertThat(q2.getType()).isEqualTo("single_choice");
            assertThat(q2.getOptions()).containsExactly("Yes", "No", "Open to discussion");

            PostQuestionVO q3 = result.getQuestions().get(2);
            assertThat(q3.getType()).isEqualTo("range");
            assertThat(q3.getMin()).isEqualTo(22);
            assertThat(q3.getMax()).isEqualTo(35);
        }

        @Test
        @DisplayName("Infers CASUAL_DATING from a dating-style post")
        void analyze_datingPost_infersDatingCategory() throws Exception {
            String claudeJson = """
                    {
                      "inferredCategory": "CASUAL_DATING",
                      "questions": [
                        {
                          "id": "q1",
                          "question": "What are you looking for?",
                          "type": "single_choice",
                          "options": ["Long-term relationship", "Casual dating", "Not sure yet"]
                        }
                      ]
                    }
                    """;

            PostAnalysisProcessor spy = spyWithClaude(claudeJson);
            PostAnalyzeResponseVO result = spy.analyze("26F looking for someone to vibe with.");

            assertThat(result.getInferredCategory()).isEqualTo(MatchCategory.CASUAL_DATING);
            assertThat(result.getQuestions()).hasSize(1);
            assertThat(result.getQuestions().get(0).getType()).isEqualTo("single_choice");
        }

        @Test
        @DisplayName("Throws MatchmakingException when Claude returns malformed JSON")
        void analyze_malformedJson_throwsException() throws Exception {
            PostAnalysisProcessor spy = spyWithClaude("this is not json {{{");

            assertThatThrownBy(() -> spy.analyze(MATRIMONIAL_POST))
                    .isInstanceOf(MatchmakingException.class)
                    .hasMessageContaining("AI analysis failed");
        }

        @Test
        @DisplayName("Parses all question types correctly")
        void analyze_allQuestionTypes_parsedCorrectly() throws Exception {
            String claudeJson = """
                    {
                      "inferredCategory": "CASUAL_DATING",
                      "questions": [
                        { "id": "q1", "question": "Age range?", "type": "range", "min": 20, "max": 35 },
                        { "id": "q2", "question": "Do you have pets?", "type": "boolean" },
                        { "id": "q3", "question": "Languages?", "type": "multi_choice", "options": ["Hindi","English","Tamil"] },
                        { "id": "q4", "question": "Your state?", "type": "dropdown", "options": ["Delhi","Maharashtra"] },
                        { "id": "q5", "question": "About yourself", "type": "text", "placeholder": "Tell us more..." }
                      ]
                    }
                    """;

            PostAnalysisProcessor spy = spyWithClaude(claudeJson);
            PostAnalyzeResponseVO result = spy.analyze("I am 25M looking to meet someone.");

            assertThat(result.getQuestions()).extracting(PostQuestionVO::getType)
                    .containsExactly("range", "boolean", "multi_choice", "dropdown", "text");
        }
    }

    // ─── submit() ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("submit()")
    class SubmitTests {

        @Test
        @DisplayName("Saves post and returns postId with inferred category")
        void submit_validRequest_savesPostAndReturnsId() throws Exception {
            PostAnalysisProcessor spy = spyWithClaude(matrimonialSubmitJson());

            UserPost savedPost = buildSavedPost(42L, MatchCategory.PROFESSIONAL_MATRIMONY);
            when(postRepo.save(any())).thenReturn(savedPost);
            when(postRepo.findById(42L)).thenReturn(Optional.of(savedPost));
            when(matchingEngine.discover(any())).thenReturn(List.of());

            PostSubmitResponseVO result = spy.submit(COGNITO_SUB, buildRequest(MATRIMONIAL_POST));

            assertThat(result.getPostId()).isEqualTo(42L);
            assertThat(result.getInferredCategory()).isEqualTo(MatchCategory.PROFESSIONAL_MATRIMONY);
            assertThat(result.getCategoryDisplayName()).isEqualTo("Matrimonial (Verified)");
        }

        @Test
        @DisplayName("Persists answers JSON alongside the post")
        void submit_withAnswers_persistsAnswersJson() throws Exception {
            PostAnalysisProcessor spy = spyWithClaude(matrimonialSubmitJson());

            UserPost saved = buildSavedPost(1L, MatchCategory.PROFESSIONAL_MATRIMONY);
            when(postRepo.save(any())).thenReturn(saved);
            when(postRepo.findById(1L)).thenReturn(Optional.of(saved));
            when(matchingEngine.discover(any())).thenReturn(List.of());

            PostAnswerVO a1 = new PostAnswerVO(); a1.setQuestionId("q1"); a1.setValue("Mumbai");
            PostAnswerVO a2 = new PostAnswerVO(); a2.setQuestionId("q2"); a2.setValue("Yes");

            PostSubmitRequestVO request = buildRequest(MATRIMONIAL_POST);
            request.setAnswers(List.of(a1, a2));
            spy.submit(COGNITO_SUB, request);

            ArgumentCaptor<UserPost> captor = ArgumentCaptor.forClass(UserPost.class);
            verify(postRepo, atLeastOnce()).save(captor.capture());
            UserPost firstSave = captor.getAllValues().get(0);
            assertThat(firstSave.getAnswersJson()).contains("q1").contains("Mumbai");
        }

        @Test
        @DisplayName("Routes PROFESSIONAL_MATRIMONY to matrimonial profile processor")
        void submit_matrimonialCategory_upsertsCategoryProfile() throws Exception {
            PostAnalysisProcessor spy = spyWithClaude(matrimonialSubmitJson());

            UserPost saved = buildSavedPost(1L, MatchCategory.PROFESSIONAL_MATRIMONY);
            when(postRepo.save(any())).thenReturn(saved);
            when(postRepo.findById(1L)).thenReturn(Optional.of(saved));
            when(matchingEngine.discover(any())).thenReturn(List.of());

            spy.submit(COGNITO_SUB, buildRequest(MATRIMONIAL_POST));
            Thread.sleep(300);

            verify(matrimonialProcessor, atLeastOnce()).upsertFromFilter(argThat(vo ->
                    COGNITO_SUB.equals(vo.getCognitoSub()) &&
                    "PROFESSIONAL_MATRIMONY".equals(vo.getChildCategory())
            ));
        }

        @Test
        @DisplayName("Routes CASUAL_DATING to dating profile processor")
        void submit_datingCategory_upsertsDatingProfile() throws Exception {
            String claudeJson = """
                    {
                      "inferredCategory": "CASUAL_DATING",
                      "profile": { "relationshipGoal": "Long-Term" }
                    }
                    """;

            PostAnalysisProcessor spy = spyWithClaude(claudeJson);

            UserPost saved = buildSavedPost(2L, MatchCategory.CASUAL_DATING);
            when(postRepo.save(any())).thenReturn(saved);
            when(postRepo.findById(2L)).thenReturn(Optional.of(saved));
            when(matchingEngine.discover(any())).thenReturn(List.of());

            spy.submit(COGNITO_SUB, buildRequest("26F looking for long-term."));
            Thread.sleep(300);

            verify(datingProcessor, atLeastOnce()).upsertFromFilter(argThat(vo ->
                    "CASUAL_DATING".equals(vo.getChildCategory())
            ));
        }

        @Test
        @DisplayName("Triggers match discovery after profile upsert")
        void submit_afterUpsert_triggersDiscovery() throws Exception {
            PostAnalysisProcessor spy = spyWithClaude(matrimonialSubmitJson());

            UserPost saved = buildSavedPost(5L, MatchCategory.PROFESSIONAL_MATRIMONY);
            when(postRepo.save(any())).thenReturn(saved);
            when(postRepo.findById(5L)).thenReturn(Optional.of(saved));
            when(matchingEngine.discover(any())).thenReturn(List.of());

            spy.submit(COGNITO_SUB, buildRequest(MATRIMONIAL_POST));
            Thread.sleep(300);

            verify(matchingEngine).discover(argThat(req ->
                    COGNITO_SUB.equals(req.getCognitoSubA()) &&
                    req.getMatchCategory() == MatchCategory.PROFESSIONAL_MATRIMONY
            ));
        }

        @Test
        @DisplayName("Sends POST_MATCH_CONNECTING when candidates found and one is online")
        void submit_candidatesFound_onlineMatch_sendsConnectingNotification() throws Exception {
            PostAnalysisProcessor spy = spyWithClaude(matrimonialSubmitJson());

            UserPost saved = buildSavedPost(7L, MatchCategory.PROFESSIONAL_MATRIMONY);
            when(postRepo.save(any())).thenReturn(saved);
            when(postRepo.findById(7L)).thenReturn(Optional.of(saved));

            MatchCandidateVO candidate = new MatchCandidateVO();
            candidate.setCognitoSubB("matched-user");
            candidate.setCompatibilityScore(85);
            when(matchingEngine.discover(any())).thenReturn(List.of(candidate));
            when(matchConnectService.connectNextOnlineMatch(COGNITO_SUB)).thenReturn(true);

            spy.submit(COGNITO_SUB, buildRequest(MATRIMONIAL_POST));
            Thread.sleep(300);

            ArgumentCaptor<MatchNotificationVO> wsCaptor = ArgumentCaptor.forClass(MatchNotificationVO.class);
            verify(messaging).convertAndSendToUser(eq(COGNITO_SUB), eq("/queue/matches"), wsCaptor.capture());
            assertThat(wsCaptor.getValue().getEvent()).isEqualTo("POST_MATCH_CONNECTING");
        }

        @Test
        @DisplayName("Sends POST_NO_MATCH_FOUND WebSocket notification when no candidates")
        void submit_noCandidates_sendsNoMatchNotification() throws Exception {
            PostAnalysisProcessor spy = spyWithClaude(matrimonialSubmitJson());

            UserPost saved = buildSavedPost(8L, MatchCategory.PROFESSIONAL_MATRIMONY);
            when(postRepo.save(any())).thenReturn(saved);
            when(postRepo.findById(8L)).thenReturn(Optional.of(saved));
            when(matchingEngine.discover(any())).thenReturn(List.of());

            spy.submit(COGNITO_SUB, buildRequest(MATRIMONIAL_POST));
            Thread.sleep(300);

            ArgumentCaptor<MatchNotificationVO> wsCaptor = ArgumentCaptor.forClass(MatchNotificationVO.class);
            verify(messaging).convertAndSendToUser(eq(COGNITO_SUB), eq("/queue/matches"), wsCaptor.capture());
            assertThat(wsCaptor.getValue().getEvent()).isEqualTo("POST_NO_MATCH_FOUND");
        }

        @Test
        @DisplayName("Throws MatchmakingException when Claude returns malformed JSON on submit")
        void submit_malformedJson_throwsException() throws Exception {
            PostAnalysisProcessor spy = spyWithClaude("{broken json");

            assertThatThrownBy(() -> spy.submit(COGNITO_SUB, buildRequest(MATRIMONIAL_POST)))
                    .isInstanceOf(MatchmakingException.class)
                    .hasMessageContaining("Failed to save post");
        }
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private PostAnalysisProcessor spyWithClaude(String responseJson) throws Exception {
        PostAnalysisProcessor spy = spy(processor);
        doReturn(responseJson).when(spy).callClaude(anyString());
        return spy;
    }

    private PostSubmitRequestVO buildRequest(String postText) {
        PostSubmitRequestVO req = new PostSubmitRequestVO();
        req.setPostText(postText);
        req.setAnswers(List.of());
        return req;
    }

    private UserPost buildSavedPost(Long id, MatchCategory category) {
        return UserPost.builder()
                .id(id)
                .cognitoSub(COGNITO_SUB)
                .postText(MATRIMONIAL_POST)
                .inferredCategory(category != null ? category.name() : null)
                .profileUpdated(false)
                .build();
    }

    private String matrimonialSubmitJson() {
        return """
                {
                  "inferredCategory": "PROFESSIONAL_MATRIMONY",
                  "profile": {
                    "religion": "Islam",
                    "nativeCity": "Delhi",
                    "profession": "Software Engineer",
                    "maritalStatus": "Never Married"
                  }
                }
                """;
    }
}
