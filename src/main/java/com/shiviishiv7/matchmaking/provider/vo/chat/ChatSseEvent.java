package com.shiviishiv7.matchmaking.provider.vo.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Final SSE event sent after streaming completes.
 * Contains structured metadata extracted from the conversation.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ChatSseEvent {

    private String type; // "token" | "metadata" | "done" | "error"

    // For type=token
    private String token;

    // For type=metadata (sent after stream completes)
    private String detectedCategory;
    private String categoryDisplayName;
    private String status;          // IN_PROGRESS | AWAITING_SUBMIT | SUBMITTED
    private Integer questionCount;
    private List<String> detectedCategories; // when multiple detected, for selection prompt

    // For type=error
    private String error;
}
