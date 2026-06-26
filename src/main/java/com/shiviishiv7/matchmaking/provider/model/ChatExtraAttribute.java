package com.shiviishiv7.matchmaking.provider.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CHAT_EXTRA_ATTRIBUTE", indexes = {
        @Index(name = "idx_cea_session_id", columnList = "sessionId"),
        @Index(name = "idx_cea_cognito_sub", columnList = "cognitoSub")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatExtraAttribute extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sessionId", nullable = false)
    private Long sessionId;

    @Column(name = "cognitoSub", nullable = false)
    private String cognitoSub;

    @Column(name = "matchCategory", nullable = false, length = 60)
    private String matchCategory;

    /** camelCase key (e.g. motivationStyle, partnerAgeVibe) */
    @Column(name = "attributeKey", nullable = false, length = 100)
    private String attributeKey;

    @Column(name = "attributeValue", columnDefinition = "TEXT")
    private String attributeValue;
}
