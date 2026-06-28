package com.shiviishiv7.matchmaking.processor.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiviishiv7.matchmaking.common.enums.IntentType;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.PartnerPreferenceRepository;
import com.shiviishiv7.matchmaking.provider.implementation.UserPostRepository;
import com.shiviishiv7.matchmaking.provider.model.UserPost;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostAnalysisProcessorTest {

    @Mock private UserPostRepository postRepo;
    @Mock private PartnerPreferenceRepository partnerPreferenceRepository;
    @Spy  private ObjectMapper objectMapper = new ObjectMapper();

    private PostAnalysisProcessor processor;

    private static final String COGNITO_SUB   = "user-123";
    private static final String MATRIMONIAL_POST =
            "27M | Delhi | Looking for a life partner who values sincerity. I work in tech.";

    @BeforeEach
    void setUp() {
        processor = new PostAnalysisProcessor(postRepo, partnerPreferenceRepository, objectMapper);
        ReflectionTestUtils.setField(processor, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(processor, "model", "claude-haiku-4-5-20251001");
    }

    // ─── analyze() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("analyze()")
    class AnalyzeTests {

        @Test
        @DisplayName("Returns only unanswered fixed questions from Claude response")
        void analyze_validPost_returnsQuestions() throws Exception {
            String claudeJson = """
                    {
                      "questions": [
                        {
                          "id": "m3",
                          "question": "Current city",
                          "type": "text",
                          "placeholder": "e.g. Delhi"
                        },
                        {
                          "id": "m6",
                          "question": "Religion",
                          "type": "dropdown",
                          "options": ["Hindu","Muslim","Christian","Sikh","Jain","Buddhist","Other"]
                        }
                      ]
                    }
                    """;

            PostAnalysisProcessor spy = spyWithClaude(claudeJson);
            PostAnalyzeResponseVO result = spy.analyze(MATRIMONIAL_POST, IntentType.MATRIMONIAL);

            assertThat(result.getQuestions()).hasSize(2);
            assertThat(result.getQuestions().get(0).getId()).isEqualTo("m3");
            assertThat(result.getQuestions().get(1).getType()).isEqualTo("dropdown");
        }

        @Test
        @DisplayName("Returns empty list if all questions are answered in post")
        void analyze_allAnswered_returnsEmpty() throws Exception {
            String claudeJson = """
                    { "questions": [] }
                    """;

            PostAnalysisProcessor spy = spyWithClaude(claudeJson);
            PostAnalyzeResponseVO result = spy.analyze(MATRIMONIAL_POST, IntentType.MATRIMONIAL);

            assertThat(result.getQuestions()).isEmpty();
        }

        @Test
        @DisplayName("Throws MatchmakingException on malformed Claude response")
        void analyze_malformedJson_throwsException() throws Exception {
            PostAnalysisProcessor spy = spyWithClaude("this is not json");

            assertThatThrownBy(() -> spy.analyze(MATRIMONIAL_POST, IntentType.MATRIMONIAL))
                    .isInstanceOf(MatchmakingException.class)
                    .hasMessageContaining("AI analysis failed");
        }
    }

    // ─── submit() ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("submit()")
    class SubmitTests {

        @Test
        @DisplayName("Saves post with ACTIVE status and 30-day expiry")
        void submit_validRequest_savesActivePost() throws Exception {
            UserPost saved = buildSavedPost(42L);
            when(postRepo.save(any())).thenReturn(saved);

            PostSubmitRequestVO request = buildRequest(MATRIMONIAL_POST, IntentType.MATRIMONIAL);
            PostSubmitResponseVO result = processor.submit(COGNITO_SUB, request);

            assertThat(result.getPostId()).isEqualTo(42L);

            ArgumentCaptor<UserPost> captor = ArgumentCaptor.forClass(UserPost.class);
            verify(postRepo).save(captor.capture());
            UserPost capturedPost = captor.getValue();
            assertThat(capturedPost.getIntent()).isEqualTo(IntentType.MATRIMONIAL);
            assertThat(capturedPost.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(29));
        }

        @Test
        @DisplayName("Persists answers JSON alongside the post")
        void submit_withAnswers_persistsAnswersJson() throws Exception {
            UserPost saved = buildSavedPost(1L);
            when(postRepo.save(any())).thenReturn(saved);

            PostAnswerVO a1 = new PostAnswerVO(); a1.setQuestionId("m3"); a1.setValue("Mumbai");
            PostSubmitRequestVO request = buildRequest(MATRIMONIAL_POST, IntentType.MATRIMONIAL);
            request.setAnswers(List.of(a1));

            processor.submit(COGNITO_SUB, request);

            ArgumentCaptor<UserPost> captor = ArgumentCaptor.forClass(UserPost.class);
            verify(postRepo).save(captor.capture());
            assertThat(captor.getValue().getAnswersJson()).contains("m3").contains("Mumbai");
        }

        @Test
        @DisplayName("Saves partner preferences when provided")
        void submit_withPartnerPrefs_savesPrefs() throws Exception {
            UserPost saved = buildSavedPost(5L);
            when(postRepo.save(any())).thenReturn(saved);

            PostSubmitRequestVO request = buildRequest(MATRIMONIAL_POST, IntentType.MATRIMONIAL);
            PartnerPreferenceRequestVO pref = new PartnerPreferenceRequestVO();
            pref.setAgeMin(25);
            pref.setAgeMax(32);
            pref.setGenderPref("Female");
            request.setPartnerPreference(pref);

            processor.submit(COGNITO_SUB, request);

            verify(partnerPreferenceRepository).save(any());
        }
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private PostAnalysisProcessor spyWithClaude(String responseJson) throws Exception {
        PostAnalysisProcessor spy = spy(processor);
        doReturn(responseJson).when(spy).callClaude(anyString());
        return spy;
    }

    private PostSubmitRequestVO buildRequest(String postText, IntentType intent) {
        PostSubmitRequestVO req = new PostSubmitRequestVO();
        req.setPostText(postText);
        req.setIntent(intent);
        req.setAnswers(List.of());
        return req;
    }

    private UserPost buildSavedPost(Long id) {
        return UserPost.builder()
                .id(id)
                .cognitoSub(COGNITO_SUB)
                .intent(IntentType.MATRIMONIAL)
                .postText(MATRIMONIAL_POST)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .matchCount(0)
                .profileUpdated(false)
                .build();
    }
}
