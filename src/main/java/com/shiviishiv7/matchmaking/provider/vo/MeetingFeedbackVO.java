package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shiviishiv7.matchmaking.common.enums.FeedbackResponse;
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
    private FeedbackResponse response;   // INTERESTED | ANOTHER_ROUND | NOT_INTERESTED
    private String notes;                // optional private note — never shown to match
}
