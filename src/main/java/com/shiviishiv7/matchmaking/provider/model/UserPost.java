package com.shiviishiv7.matchmaking.provider.model;

import com.shiviishiv7.matchmaking.common.enums.IntentType;
import com.shiviishiv7.matchmaking.common.enums.PostStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "USER_POSTS", indexes = {
        @Index(name = "idx_user_posts_cognito_sub", columnList = "cognitoSub"),
        @Index(name = "idx_user_posts_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "cognitoSub", nullable = false)
    private String cognitoSub;

    @Enumerated(EnumType.STRING)
    @Column(name = "intent", nullable = false, length = 20)
    private IntentType intent;

    @Column(name = "postText", nullable = false, columnDefinition = "TEXT")
    private String postText;

    @Column(name = "answersJson", columnDefinition = "JSON")
    private String answersJson;

    @Column(name = "inferredCategory", length = 60)
    private String inferredCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PostStatus status = PostStatus.ACTIVE;

    @Column(name = "matchCount", nullable = false)
    @Builder.Default
    private Integer matchCount = 0;

    @Column(name = "expiresAt")
    private LocalDateTime expiresAt;

    @Column(name = "profileUpdated")
    @Builder.Default
    private Boolean profileUpdated = false;
}
