package com.shiviishiv7.matchmaking.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.common.util.security.CurrentUserContext;
import com.shiviishiv7.matchmaking.common.util.security.CurrentUserDetails;
import com.shiviishiv7.matchmaking.controller.UserPostController;
import com.shiviishiv7.matchmaking.processor.post.IPostAnalysisProcessor;
import com.shiviishiv7.matchmaking.provider.vo.post.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserPostController — verifies HTTP layer routing,
 * request delegation, and response wrapping.
 */
@ExtendWith(MockitoExtension.class)
class UserPostControllerTest {

    @Mock private IPostAnalysisProcessor postAnalysisProcessor;

    @InjectMocks private UserPostController controller;

    private static final String COGNITO_SUB = "user-abc";

    @BeforeEach
    void setUpSecurityContext() {
        CurrentUserDetails user = new CurrentUserDetails();
        user.setUsername(COGNITO_SUB);
        CurrentUserContext.setCurrentUser(user);
    }

    @AfterEach
    void clearSecurityContext() {
        CurrentUserContext.clear();
    }

    // ─── POST /api/v1/posts/analyze ───────────────────────────────────────────

    @Test
    @DisplayName("analyze() returns 200 with questions and inferred category")
    void analyze_validRequest_returns200WithQuestions() {
        PostAnalyzeResponseVO analyzeResponse = PostAnalyzeResponseVO.builder()
                .inferredCategory(MatchCategory.PROFESSIONAL_MATRIMONY)
                .categoryDisplayName("Matrimonial (Verified)")
                .questions(List.of(
                        PostQuestionVO.builder().id("q1").question("City?").type("text").build(),
                        PostQuestionVO.builder().id("q2").question("Relocate?").type("boolean").build()
                ))
                .build();

        when(postAnalysisProcessor.analyze(anyString())).thenReturn(analyzeResponse);

        PostAnalyzeRequestVO request = new PostAnalyzeRequestVO();
        request.setPostText("27M | Delhi | Looking for a sincere life partner.");

        ResponseEntity<?> response = controller.analyze(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(postAnalysisProcessor).analyze("27M | Delhi | Looking for a sincere life partner.");
    }

    @Test
    @DisplayName("analyze() passes post text to processor unchanged")
    void analyze_delegatesPostTextToProcessor() {
        String postText = "25F software engineer looking for companionship.";
        when(postAnalysisProcessor.analyze(postText))
                .thenReturn(PostAnalyzeResponseVO.builder().questions(List.of()).build());

        PostAnalyzeRequestVO request = new PostAnalyzeRequestVO();
        request.setPostText(postText);

        controller.analyze(request);

        verify(postAnalysisProcessor, times(1)).analyze(postText);
    }

    @Test
    @DisplayName("analyze() propagates MatchmakingException from processor")
    void analyze_processorThrows_exceptionPropagates() {
        when(postAnalysisProcessor.analyze(anyString()))
                .thenThrow(new MatchmakingException("AI analysis failed. Please try again.", 500));

        PostAnalyzeRequestVO request = new PostAnalyzeRequestVO();
        request.setPostText("Some post text here to meet the minimum length.");

        assertThatThrownBy(() -> controller.analyze(request))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("AI analysis failed");
    }

    // ─── POST /api/v1/posts/submit ────────────────────────────────────────────

    @Test
    @DisplayName("submit() returns 200 with postId and category")
    void submit_validRequest_returns200WithPostId() {
        PostSubmitResponseVO submitResponse = PostSubmitResponseVO.builder()
                .postId(99L)
                .inferredCategory(MatchCategory.PROFESSIONAL_MATRIMONY)
                .categoryDisplayName("Matrimonial (Verified)")
                .profileUpdated(true)
                .build();

        when(postAnalysisProcessor.submit(eq(COGNITO_SUB), any())).thenReturn(submitResponse);

        PostSubmitRequestVO request = buildSubmitRequest();
        ResponseEntity<?> response = controller.submit(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(postAnalysisProcessor).submit(eq(COGNITO_SUB), any());
    }

    @Test
    @DisplayName("submit() passes cognitoSub from security context to processor")
    void submit_usesCognitoSubFromSecurityContext() {
        when(postAnalysisProcessor.submit(eq(COGNITO_SUB), any()))
                .thenReturn(PostSubmitResponseVO.builder().postId(1L).profileUpdated(false).build());

        controller.submit(buildSubmitRequest());

        verify(postAnalysisProcessor).submit(eq(COGNITO_SUB), any());
        verifyNoMoreInteractions(postAnalysisProcessor);
    }

    @Test
    @DisplayName("submit() passes answers to processor")
    void submit_withAnswers_passesAnswersToProcessor() {
        PostAnswerVO a1 = new PostAnswerVO(); a1.setQuestionId("q1"); a1.setValue("Delhi");
        PostAnswerVO a2 = new PostAnswerVO(); a2.setQuestionId("q2"); a2.setValue("true");

        when(postAnalysisProcessor.submit(eq(COGNITO_SUB), any()))
                .thenReturn(PostSubmitResponseVO.builder().postId(5L).profileUpdated(true).build());

        PostSubmitRequestVO request = buildSubmitRequest();
        request.setAnswers(List.of(a1, a2));

        controller.submit(request);

        verify(postAnalysisProcessor).submit(eq(COGNITO_SUB), argThat(req ->
                req.getAnswers().size() == 2 &&
                req.getAnswers().get(0).getQuestionId().equals("q1")
        ));
    }

    @Test
    @DisplayName("submit() propagates MatchmakingException from processor")
    void submit_processorThrows_exceptionPropagates() {
        when(postAnalysisProcessor.submit(anyString(), any()))
                .thenThrow(new MatchmakingException("Failed to save post. Please try again.", 500));

        assertThatThrownBy(() -> controller.submit(buildSubmitRequest()))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("Failed to save post");
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private PostSubmitRequestVO buildSubmitRequest() {
        PostSubmitRequestVO req = new PostSubmitRequestVO();
        req.setPostText("27M | Delhi | Looking for a sincere life partner.");
        req.setAnswers(List.of());
        return req;
    }
}
