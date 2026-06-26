package com.shiviishiv7.matchmaking.provider.vo.chat;

import com.shiviishiv7.matchmaking.provider.model.ChatSession;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class ChatSessionVO {

    private Long id;
    private String detectedCategory;
    private String status;
    private Integer questionCount;
    private List<ChatMessageVO> conversationHistory;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ChatSessionVO from(ChatSession session, List<ChatMessageVO> history) {
        return ChatSessionVO.builder()
                .id(session.getId())
                .detectedCategory(session.getDetectedCategory())
                .status(session.getStatus())
                .questionCount(session.getQuestionCount())
                .conversationHistory(history)
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}
