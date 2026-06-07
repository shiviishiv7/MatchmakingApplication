package com.shiviishiv7.matchmaking.provider.vo.ws;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Sent to a user over WebSocket to notify them of the result of their instant match search.
 *
 * event values:
 *   MATCH_FOUND    — compatible user found; WebRTC session can start
 *   NO_MATCH_FOUND — no one available right now; added to waiting queue
 *   MATCH_LATER    — a match was found for a previously waiting user
 */
@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchNotificationVO {

    private String event;           // MATCH_FOUND | NO_MATCH_FOUND | MATCH_LATER
    private String matchId;
    private String matchedUserId;
    private String matchedUserName;
    private String matchedUserProfilePic;
    private Double compatibilityScore;
    private String message;
}
