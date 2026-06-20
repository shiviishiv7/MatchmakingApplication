package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingType;
import com.shiviishiv7.matchmaking.provider.model.Match;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;



@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchVO {

    private Integer id;

    private String cognitoSubA;
    private String cognitoSubB;
    private MatchStatus status;
    private Double compatibilityScore;
    private Integer roundCount;
    private Integer maxRounds;
    private MeetingType meetingType;

    public boolean validate() {
        if (cognitoSubA == null) {
            throw new IllegalArgumentException("User A ID cannot be null");
        }
        if (cognitoSubB == null) {
            throw new IllegalArgumentException("User B ID cannot be null");
        }
        if (cognitoSubB.equals(cognitoSubA)) {
            throw new IllegalArgumentException("User A and User B cannot be the same user");
        }
        return true;
    }

    public Match fromVO() {
        Match match = new Match();
        match.setId(id);
        match.setStatus(status);
        match.setCompatibilityScore(compatibilityScore);
        match.setRoundCount(roundCount);
        match.setMaxRounds(maxRounds);
        if (meetingType != null) match.setMeetingType(meetingType);
        return match;
    }

    public MatchVO toVO(Match match) {
        MatchVO vo = new MatchVO();
        vo.setId(match.getId());
        vo.setCognitoSubB(match.getCognitoSubB());
        vo.setCognitoSubA(match.getCognitoSubA());
        vo.setStatus(match.getStatus());
        vo.setCompatibilityScore(match.getCompatibilityScore());
        vo.setRoundCount(match.getRoundCount());
        vo.setMaxRounds(match.getMaxRounds());
        vo.setMeetingType(match.getMeetingType());
        return vo;
    }
}
