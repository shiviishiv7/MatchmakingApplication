package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shiviishiv7.matchmaking.common.enums.FeedbackResponse;
import com.shiviishiv7.matchmaking.provider.model.MeetingFeedback;
import lombok.Getter;
import lombok.Setter;



@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeetingFeedbackVO {

    private Integer id;
    private String meetingId;
    private String userId;
    private FeedbackResponse response;
    private String notes;

    public boolean validate() {
        if (meetingId == null) {
            throw new IllegalArgumentException("Meeting ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (response == null) {
            throw new IllegalArgumentException("Response cannot be null");
        }
        return true;
    }

    public MeetingFeedback fromVO() {
        MeetingFeedback feedback = new MeetingFeedback();
        feedback.setId(id);
        feedback.setResponse(response);
        feedback.setNotes(notes);
        return feedback;
    }

    public MeetingFeedbackVO toVO(MeetingFeedback feedback) {
        MeetingFeedbackVO vo = new MeetingFeedbackVO();
        vo.setId(feedback.getId());
//        vo.setMeetingId(feedback.getMeeting() != null ? feedback.getMeeting().getId() : null);
//        vo.setUserId(feedback.getUser() != null ? feedback.getUser().getId() : null);
        vo.setResponse(feedback.getResponse());
        vo.setNotes(feedback.getNotes());
        return vo;
    }
}
