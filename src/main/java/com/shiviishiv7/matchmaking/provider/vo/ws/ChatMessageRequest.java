package com.shiviishiv7.matchmaking.provider.vo.ws;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private String toUserId;   // Cognito sub of the target user
    private String message;    // the actual message content
}
