package com.shiviishiv7.matchmaking.provider.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CHAT_QUESTION_BANK", indexes = {
        @Index(name = "idx_cqb_category", columnList = "matchCategory")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatQuestionBank extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "matchCategory", nullable = false, length = 60)
    private String matchCategory;

    @Column(name = "questionText", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    /** ExtProfile field this question maps to (e.g. fitnessLevel). Null if unmapped. */
    @Column(name = "mappedAttribute", length = 100)
    private String mappedAttribute;

    @Column(name = "isActive")
    @Builder.Default
    private Boolean isActive = true;
}
