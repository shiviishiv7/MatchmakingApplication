package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shiviishiv7.matchmaking.common.enums.MeetingStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingType;
import com.shiviishiv7.matchmaking.provider.model.Meeting;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeetingVO {

    private Integer id;
    private Integer matchResultId;
    private Integer roundNumber;
    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
    private MeetingStatus status;
    private MeetingType meetingType;

    // Peer info — populated by getUpcomingMeetings
    private String peerFirstName;
    private String peerLastName;
    private String peerCognitoSub;

    public boolean validate() {
        if (matchResultId == null) {
            throw new IllegalArgumentException("Match result ID cannot be null");
        }
        if (roundNumber == null || roundNumber <= 0) {
            throw new IllegalArgumentException("Round number must be greater than 0");
        }
        if (meetingType != MeetingType.INSTANT && scheduledAt == null) {
            throw new IllegalArgumentException("Scheduled time cannot be null for non-instant meetings");
        }
        return true;
    }

    public Meeting fromVO() {
        Meeting meeting = new Meeting();
        meeting.setId(id);
        meeting.setMatchResultId(matchResultId);
        meeting.setRoundNumber(roundNumber);
        meeting.setScheduledAt(scheduledAt);
        meeting.setDurationMinutes(durationMinutes);
        meeting.setStatus(status);
        if (meetingType != null) meeting.setMeetingType(meetingType);
        return meeting;
    }

    public MeetingVO toVO(Meeting meeting) {
        MeetingVO vo = new MeetingVO();
        vo.setId(meeting.getId());
        vo.setMatchResultId(meeting.getMatchResultId());
        vo.setRoundNumber(meeting.getRoundNumber());
        vo.setScheduledAt(meeting.getScheduledAt());
        vo.setDurationMinutes(meeting.getDurationMinutes());
        vo.setStatus(meeting.getStatus());
        vo.setMeetingType(meeting.getMeetingType());
        return vo;
    }
}
