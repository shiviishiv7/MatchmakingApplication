package com.shiviishiv7.matchmaking.provider.vo.ws;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Sent by the client to /app/meeting/join-waiting-room
 * when they open the scheduled call screen.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class WaitingRoomJoinVO {

    private String meetingId;
    private String userId;  // cognitoSub of the joining user
}
