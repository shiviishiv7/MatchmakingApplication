package com.shiviishiv7.matchmaking.provider.vo.ws;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * A lightweight view of a user shown in the available-users pool.
 */
@Data
@AllArgsConstructor
public class PoolUserVO {
    private String cognitoSub;   // used as the user identifier for WebRTC signaling
    private String firstName;
    private String lastName;
//    private String industry;
}
