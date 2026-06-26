package com.shiviishiv7.matchmaking.processor.match;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.MatchResultRepository;
import com.shiviishiv7.matchmaking.provider.model.MatchResult;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchProcessorTest {

    @Mock private MatchResultRepository matchRepository;
    @InjectMocks private MatchProcessor processor;

    // ── get ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("get: returns match VO when found")
    void get_exists_returnsVO() throws MatchmakingException {
        when(matchRepository.findById(1)).thenReturn(Optional.of(buildMatch(1, MatchStatus.PENDING)));
        BaseVO result = processor.get("1");
        assertThat(result).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("get: throws DATA_NOT_FOUND when match missing")
    void get_notFound_throws() {
        when(matchRepository.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> processor.get("99"))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("Match does not exist");
    }

    // ── getActiveMatchForUser ─────────────────────────────────────────────────

    @Test
    @DisplayName("getActiveMatchForUser: returns active match VO")
    void getActiveMatchForUser_found_returnsVO() throws MatchmakingException {
        MatchResult match = buildMatch(1, MatchStatus.MEETING_SCHEDULED);
        when(matchRepository.findMatchByCognitoSubAOrCognitoSubB("sub-a", "sub-a"))
                .thenReturn(Optional.of(match));
        BaseVO result = processor.getActiveMatchForUser("sub-a");
        assertThat(result.getStatusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("getActiveMatchForUser: throws when no active match")
    void getActiveMatchForUser_notFound_throws() {
        when(matchRepository.findMatchByCognitoSubAOrCognitoSubB(any(), any()))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> processor.getActiveMatchForUser("sub-a"))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("No active match found");
    }

    // ── getAllByStatus ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllByStatus: returns list for valid status")
    void getAllByStatus_validStatus_returnsList() throws MatchmakingException {
        when(matchRepository.findByStatus(MatchStatus.PENDING))
                .thenReturn(List.of(buildMatch(1, MatchStatus.PENDING)));
        BaseVO result = processor.getAllByStatus("PENDING");
        assertThat(result.getStatusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("getAllByStatus: throws for invalid status string")
    void getAllByStatus_invalidStatus_throws() {
        assertThatThrownBy(() -> processor.getAllByStatus("SWIPED_LEFT"))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("Invalid match status");
    }

    @Test
    @DisplayName("getAllByStatus: throws VALIDATION_ERROR for blank status")
    void getAllByStatus_blank_throws() {
        assertThatThrownBy(() -> processor.getAllByStatus(""))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("Status cannot be empty");
    }

    @Test
    @DisplayName("getAllByStatus: throws DATA_NOT_FOUND when list is empty")
    void getAllByStatus_noResults_throws() {
        when(matchRepository.findByStatus(MatchStatus.ENDED)).thenReturn(Collections.emptyList());
        assertThatThrownBy(() -> processor.getAllByStatus("ENDED"))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("No matches found");
    }

    // ── end ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("end: transitions PENDING match to ENDED")
    void end_pending_transitions() throws MatchmakingException {
        MatchResult match = buildMatch(1, MatchStatus.PENDING);
        when(matchRepository.findById(1)).thenReturn(Optional.of(match));
        processor.end("1");
        assertThat(match.getStatus()).isEqualTo(MatchStatus.ENDED);
        verify(matchRepository).save(match);
    }

    @Test
    @DisplayName("end: transitions MEETING_SCHEDULED match to ENDED")
    void end_meetingScheduled_transitions() throws MatchmakingException {
        MatchResult match = buildMatch(2, MatchStatus.MEETING_SCHEDULED);
        when(matchRepository.findById(2)).thenReturn(Optional.of(match));
        processor.end("2");
        assertThat(match.getStatus()).isEqualTo(MatchStatus.ENDED);
    }

    @Test
    @DisplayName("end: throws VALIDATION_ERROR when match already ENDED")
    void end_alreadyEnded_throws() {
        when(matchRepository.findById(3)).thenReturn(Optional.of(buildMatch(3, MatchStatus.ENDED)));
        assertThatThrownBy(() -> processor.end("3"))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("terminal state");
    }

    @Test
    @DisplayName("end: throws VALIDATION_ERROR when match already COMPLETED")
    void end_alreadyCompleted_throws() {
        when(matchRepository.findById(4)).thenReturn(Optional.of(buildMatch(4, MatchStatus.COMPLETED)));
        assertThatThrownBy(() -> processor.end("4"))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("terminal state");
    }

    @Test
    @DisplayName("end: throws DATA_NOT_FOUND when match missing")
    void end_notFound_throws() {
        when(matchRepository.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> processor.end("99"))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("Match does not exist");
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private MatchResult buildMatch(int id, MatchStatus status) {
        return MatchResult.builder()
                .id(id)
                .cognitoSubA("sub-a")
                .cognitoSubB("sub-b")
                .matchCategory(MatchCategory.CASUAL_DATING)
                .status(status)
                .build();
    }
}
