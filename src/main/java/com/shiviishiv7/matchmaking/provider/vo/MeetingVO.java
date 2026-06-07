package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shiviishiv7.matchmaking.common.enums.MeetingStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingType;
import com.shiviishiv7.matchmaking.provider.model.Meeting;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeetingVO {

    private UUID id;
    private UUID matchId;
    private Integer roundNumber;
    private String zoomMeetingId;
    private String zoomJoinUrl;
    private String zoomPassword;
    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
    private MeetingStatus status;
    private MeetingType meetingType;

    public boolean validate() {
        if (matchId == null) {
            throw new IllegalArgumentException("Match ID cannot be null");
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
        meeting.setRoundNumber(roundNumber);
        meeting.setZoomMeetingId(zoomMeetingId);
        meeting.setZoomJoinUrl(zoomJoinUrl);
        meeting.setZoomPassword(zoomPassword);
        meeting.setScheduledAt(scheduledAt);
        meeting.setDurationMinutes(durationMinutes);
        meeting.setStatus(status);
        if (meetingType != null) meeting.setMeetingType(meetingType);
        return meeting;
    }

    public MeetingVO toVO(Meeting meeting) {
        MeetingVO vo = new MeetingVO();
        vo.setId(meeting.getId());
        vo.setMatchId(meeting.getMatch() != null ? meeting.getMatch().getId() : null);
        vo.setRoundNumber(meeting.getRoundNumber());
        vo.setZoomMeetingId(meeting.getZoomMeetingId());
        vo.setZoomJoinUrl(meeting.getZoomJoinUrl());
        vo.setZoomPassword(meeting.getZoomPassword());
        vo.setScheduledAt(meeting.getScheduledAt());
        vo.setDurationMinutes(meeting.getDurationMinutes());
        vo.setStatus(meeting.getStatus());
        vo.setMeetingType(meeting.getMeetingType());
        return vo;
    }
}
