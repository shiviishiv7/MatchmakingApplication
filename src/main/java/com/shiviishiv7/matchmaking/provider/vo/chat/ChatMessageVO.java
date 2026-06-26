package com.shiviishiv7.matchmaking.provider.vo.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ChatMessageVO {
    private String role;   // "user" or "assistant"
    private String content;
}
