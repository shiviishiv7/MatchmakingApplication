package com.shiviishiv7.matchmaking.provider.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "USER_POSTS", indexes = {
        @Index(name = "idx_user_posts_cognito_sub", columnList = "cognitoSub")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "cognitoSub", nullable = false)
    private String cognitoSub;

    @Column(name = "postText", nullable = false, columnDefinition = "TEXT")
    private String postText;

    @Column(name = "answersJson", columnDefinition = "JSON")
    private String answersJson;

    // Stored as VARCHAR so adding new categories never requires an ALTER TABLE
    @Column(name = "inferredCategory", length = 60)
    private String inferredCategory;

    @Column(name = "profileUpdated")
    @Builder.Default
    private Boolean profileUpdated = false;
}
