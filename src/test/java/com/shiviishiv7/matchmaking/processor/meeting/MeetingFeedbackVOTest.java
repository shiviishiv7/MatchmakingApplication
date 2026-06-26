package com.shiviishiv7.matchmaking.processor.meeting;

import com.shiviishiv7.matchmaking.common.enums.FeedbackResponse;
import com.shiviishiv7.matchmaking.provider.model.MeetingFeedback;
import com.shiviishiv7.matchmaking.provider.vo.MeetingFeedbackVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for MeetingFeedbackVO — validation and model mapping
 * that form the input boundary of the post-match feedback flow.
 */
class MeetingFeedbackVOTest {

    // ── validate ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("validate: passes when all required fields are set")
    void validate_allFieldsPresent_returnsTrue() {
        MeetingFeedbackVO vo = buildValidVO();
        assertThat(vo.validate()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("validate: throws when meetingId is blank or null")
    void validate_blankMeetingId_throws(String meetingId) {
        MeetingFeedbackVO vo = buildValidVO();
        vo.setMeetingId(meetingId);
        assertThatThrownBy(vo::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("meetingId");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("validate: throws when cognitoSub is blank or null")
    void validate_blankCognitoSub_throws(String cognitoSub) {
        MeetingFeedbackVO vo = buildValidVO();
        vo.setCognitoSub(cognitoSub);
        assertThatThrownBy(vo::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cognitoSub");
    }

    @Test
    @DisplayName("validate: throws when response is null")
    void validate_nullResponse_throws() {
        MeetingFeedbackVO vo = buildValidVO();
        vo.setResponse(null);
        assertThatThrownBy(vo::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("response");
    }

    // ── validate: each FeedbackResponse value ─────────────────────────────────

    @Test
    @DisplayName("validate: accepts YES response")
    void validate_yes_passes() {
        MeetingFeedbackVO vo = buildValidVO();
        vo.setResponse(FeedbackResponse.YES);
        assertThat(vo.validate()).isTrue();
    }

    @Test
    @DisplayName("validate: accepts NO response")
    void validate_no_passes() {
        MeetingFeedbackVO vo = buildValidVO();
        vo.setResponse(FeedbackResponse.NO);
        assertThat(vo.validate()).isTrue();
    }

    @Test
    @DisplayName("validate: accepts NO response (duplicate removed)")
    void validate_no_passes_2() {
        MeetingFeedbackVO vo = buildValidVO();
        vo.setResponse(FeedbackResponse.NO);
        assertThat(vo.validate()).isTrue();
    }

    // ── fromVO ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("fromVO: maps all fields to MeetingFeedback entity correctly")
    void fromVO_mapsAllFields() {
        MeetingFeedbackVO vo = buildValidVO();
        vo.setNotes("They seemed nice");

        MeetingFeedback entity = vo.fromVO();

        assertThat(entity.getMeetingId()).isEqualTo("meeting-42");
        assertThat(entity.getCognitoSub()).isEqualTo("sub-user-1");
        assertThat(entity.getResponse()).isEqualTo(FeedbackResponse.YES);
        assertThat(entity.getNotes()).isEqualTo("They seemed nice");
    }

    @Test
    @DisplayName("fromVO: maps null notes without error")
    void fromVO_nullNotes_entityNotesIsNull() {
        MeetingFeedbackVO vo = buildValidVO();
        vo.setNotes(null);

        MeetingFeedback entity = vo.fromVO();

        assertThat(entity.getNotes()).isNull();
    }

    // ── toVO ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("toVO: maps MeetingFeedback entity back to VO correctly")
    void toVO_mapsAllFields() {
        MeetingFeedback entity = MeetingFeedback.builder()
                .id(7)
                .meetingId("meeting-42")
                .cognitoSub("sub-user-1")
                .response(FeedbackResponse.NO)
                .notes("Want one more meeting")
                .build();

        MeetingFeedbackVO vo = new MeetingFeedbackVO().toVO(entity);

        assertThat(vo.getId()).isEqualTo(7);
        assertThat(vo.getMeetingId()).isEqualTo("meeting-42");
        assertThat(vo.getCognitoSub()).isEqualTo("sub-user-1");
        assertThat(vo.getResponse()).isEqualTo(FeedbackResponse.NO);
        assertThat(vo.getNotes()).isEqualTo("Want one more meeting");
    }

    @Test
    @DisplayName("toVO: getUserId() returns cognitoSub")
    void toVO_getUserId_returnsCognitoSub() {
        MeetingFeedbackVO vo = buildValidVO();
        assertThat(vo.getUserId()).isEqualTo("sub-user-1");
    }

    // ── round-trip ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("fromVO → toVO round-trip preserves all values")
    void roundTrip_fromVOThenToVO_preservesValues() {
        MeetingFeedbackVO original = buildValidVO();
        original.setNotes("Notes here");

        MeetingFeedback entity = original.fromVO();
        MeetingFeedbackVO restored = new MeetingFeedbackVO().toVO(entity);

        assertThat(restored.getMeetingId()).isEqualTo(original.getMeetingId());
        assertThat(restored.getCognitoSub()).isEqualTo(original.getCognitoSub());
        assertThat(restored.getResponse()).isEqualTo(original.getResponse());
        assertThat(restored.getNotes()).isEqualTo(original.getNotes());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private MeetingFeedbackVO buildValidVO() {
        MeetingFeedbackVO vo = new MeetingFeedbackVO();
        vo.setMeetingId("meeting-42");
        vo.setCognitoSub("sub-user-1");
        vo.setResponse(FeedbackResponse.YES);
        return vo;
    }
}
