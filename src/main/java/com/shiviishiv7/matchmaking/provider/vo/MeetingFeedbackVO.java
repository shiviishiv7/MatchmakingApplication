package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shiviishiv7.matchmaking.common.enums.FeedbackResponse;
import com.shiviishiv7.matchmaking.provider.model.MeetingFeedback;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeetingFeedbackVO {

    private Integer id;
    private String meetingId;
    private String cognitoSub;
    private FeedbackResponse response;
    private String notes;

    public String getUserId() {
        return cognitoSub;
    }

    public boolean validate() {
        if (meetingId == null || meetingId.isBlank()) throw new IllegalArgumentException("meetingId is required.");
        if (cognitoSub == null || cognitoSub.isBlank()) throw new IllegalArgumentException("cognitoSub is required.");
        if (response == null) throw new IllegalArgumentException("response is required.");
        return true;
    }

    public MeetingFeedback fromVO() {
        return MeetingFeedback.builder()
                .id(this.id)
                .meetingId(this.meetingId)
                .cognitoSub(this.cognitoSub)
                .response(this.response)
                .notes(this.notes)
                .build();
    }

    public MeetingFeedbackVO toVO(MeetingFeedback feedback) {
        MeetingFeedbackVO vo = new MeetingFeedbackVO();
        vo.setId(feedback.getId());
        vo.setMeetingId(feedback.getMeetingId());
        vo.setCognitoSub(feedback.getCognitoSub());
        vo.setResponse(feedback.getResponse());
        vo.setNotes(feedback.getNotes());
        return vo;
    }
}
