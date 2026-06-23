package com.shiviishiv7.matchmaking.engine;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.processor.matchingengine.CategoryScorer;
import com.shiviishiv7.matchmaking.processor.matchingengine.MatchingEngineProcessor;
import com.shiviishiv7.matchmaking.provider.implementation.BlockListRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MatchResultRepository;
import com.shiviishiv7.matchmaking.provider.model.MatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MatchingEngineProcessor.recordAction() —
 * validates that mutual match detection works correctly after the
 * countMutualLike fix (enum param instead of string literal).
 */
@ExtendWith(MockitoExtension.class)
class MatchingEngineProcessorTest {

    @Mock
    private MatchResultRepository matchResultRepository;

    @Mock
    private BlockListRepository blockListRepository;

    @Mock
    private CategoryScorer dummyScorer;

    private MatchingEngineProcessor engine;

    private static final String USER_A = "sub-a";
    private static final String USER_B = "sub-b";
    private static final MatchCategory CAT = MatchCategory.CASUAL_DATING;

    @BeforeEach
    void setUp() {
        when(dummyScorer.supports()).thenReturn(CAT);
        engine = new MatchingEngineProcessor(List.of(dummyScorer));

        // Inject mocked repos via reflection-friendly constructor approach
        injectField(engine, "matchResultRepository", matchResultRepository);
        injectField(engine, "blockListRepository", blockListRepository);
    }

    // ── recordAction: LIKE with no mutual ─────────────────────────────────────

    @Test
    @DisplayName("When A likes B and B has not liked A, status stays LIKED — no mutual match")
    void recordAction_aLikesB_noMutual_staysLiked() throws MatchmakingException {
        MatchResult existing = buildResult(USER_A, USER_B, MatchStatus.PENDING);
        when(matchResultRepository.findByCognitoSubAAndCognitoSubBAndMatchCategory(USER_A, USER_B, CAT))
                .thenReturn(Optional.of(existing));

        // B has NOT liked A yet
        when(matchResultRepository.countMutualLike(USER_B, USER_A, CAT, MatchStatus.LIKED))
                .thenReturn(0L);

        engine.recordAction(USER_A, USER_B, CAT, MatchStatus.LIKED);

        assertThat(existing.getStatus()).isEqualTo(MatchStatus.LIKED);
        assertThat(existing.getIsMutual()).isFalse();
        verify(matchResultRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("When A likes B and B already liked A, both rows become CONNECTED with isMutual=true")
    void recordAction_mutualLike_bothBecomeConnected() throws MatchmakingException {
        MatchResult aRow = buildResult(USER_A, USER_B, MatchStatus.PENDING);
        MatchResult bRow = buildResult(USER_B, USER_A, MatchStatus.LIKED);

        when(matchResultRepository.findByCognitoSubAAndCognitoSubBAndMatchCategory(USER_A, USER_B, CAT))
                .thenReturn(Optional.of(aRow));

        // B already liked A → mutual = 1
        when(matchResultRepository.countMutualLike(USER_B, USER_A, CAT, MatchStatus.LIKED))
                .thenReturn(1L);

        // B's row lookup for mirror update
        when(matchResultRepository.findByCognitoSubAAndCognitoSubBAndMatchCategory(USER_B, USER_A, CAT))
                .thenReturn(Optional.of(bRow));

        engine.recordAction(USER_A, USER_B, CAT, MatchStatus.LIKED);

        // A's row
        assertThat(aRow.getStatus()).isEqualTo(MatchStatus.CONNECTED);
        assertThat(aRow.getIsMutual()).isTrue();

        // B's mirror row
        assertThat(bRow.getStatus()).isEqualTo(MatchStatus.CONNECTED);
        assertThat(bRow.getIsMutual()).isTrue();
    }

    @Test
    @DisplayName("When A SKIPs B, mutual check is never called and status becomes SKIPPED")
    void recordAction_skip_noMutualCheck() throws MatchmakingException {
        MatchResult existing = buildResult(USER_A, USER_B, MatchStatus.PENDING);
        when(matchResultRepository.findByCognitoSubAAndCognitoSubBAndMatchCategory(USER_A, USER_B, CAT))
                .thenReturn(Optional.of(existing));

        engine.recordAction(USER_A, USER_B, CAT, MatchStatus.SKIPPED);

        assertThat(existing.getStatus()).isEqualTo(MatchStatus.SKIPPED);
        // countMutualLike must never be called for a SKIP
        verify(matchResultRepository, never()).countMutualLike(any(), any(), any(), any());
    }

    @Test
    @DisplayName("recordAction creates a new MatchResult on-the-fly if user acted via deep link (no existing row)")
    void recordAction_noExistingRow_createsOnTheFly() throws MatchmakingException {
        when(matchResultRepository.findByCognitoSubAAndCognitoSubBAndMatchCategory(USER_A, USER_B, CAT))
                .thenReturn(Optional.empty());
        when(matchResultRepository.countMutualLike(USER_B, USER_A, CAT, MatchStatus.LIKED))
                .thenReturn(0L);

        engine.recordAction(USER_A, USER_B, CAT, MatchStatus.LIKED);

        // A new MatchResult should have been saved
        verify(matchResultRepository).save(argThat(r ->
                USER_A.equals(r.getCognitoSubA()) &&
                USER_B.equals(r.getCognitoSubB()) &&
                r.getStatus() == MatchStatus.LIKED
        ));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private MatchResult buildResult(String subA, String subB, MatchStatus status) {
        return MatchResult.builder()
                .cognitoSubA(subA)
                .cognitoSubB(subB)
                .matchCategory(CAT)
                .status(status)
                .isMutual(false)
                .shownAt(LocalDateTime.now())
                .build();
    }

    /** Injects a field via reflection (avoids needing @InjectMocks with constructor injection). */
    private void injectField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Could not inject field: " + fieldName, e);
        }
    }
}
