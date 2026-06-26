package com.shiviishiv7.matchmaking.provider.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CHAT_SESSION", indexes = {
        @Index(name = "idx_chat_session_cognito_sub", columnList = "cognitoSub"),
        @Index(name = "idx_chat_session_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cognitoSub", nullable = false)
    private String cognitoSub;

    @Column(name = "detectedCategory", length = 60)
    private String detectedCategory;

    @Column(name = "conversationHistory", columnDefinition = "LONGTEXT")
    private String conversationHistory;

    @Column(name = "collectedAttributes", columnDefinition = "TEXT")
    private String collectedAttributes;

    @Column(name = "questionCount")
    @Builder.Default
    private Integer questionCount = 0;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "IN_PROGRESS";
}
