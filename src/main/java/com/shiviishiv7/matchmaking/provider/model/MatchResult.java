package com.shiviishiv7.matchmaking.provider.model;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.provider.vo.MatchResultVO;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Persisted record of a match suggestion shown to a user.
 * Created when a candidate appears in a user's discovery results.
 * Updated when the user acts on it (liked, skipped, connected).
 */
@Entity
@Table(name = "matchResult")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MatchResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    // ── The two matched users ─────────────────────────────────────────────
    @Column(name = "cognitoSubA", nullable = false)
    private String cognitoSubA;

    @Column(name = "cognitoSubB", nullable = false)
    private String cognitoSubB;

    @Enumerated(EnumType.STRING)
    @Column(name = "matchCategory", nullable = false, length = 60)
    private MatchCategory matchCategory;

    @Column(name = "compatibilityScore")
    private Double compatibilityScore;            // 0-100

    @Column(name = "scoreBreakdown", columnDefinition = "JSON")
    private String scoreBreakdown;                 // JSON: {religion:20, caste:15, ...}

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private MatchStatus status = MatchStatus.PENDING;

    @Column(name = "isMutual")
    @Builder.Default
    private Boolean isMutual = false;              // true when both users liked each other

    @Column(name = "shownAt")
    private LocalDateTime shownAt;

    @Column(name = "actedAt")
    private LocalDateTime actedAt;
    @Column(name = "roundCount")
    private Integer roundCount;
    @Column(name = "maxRounds")
    private Integer maxRounds;

    public MatchResult fromVO(MatchResultVO vo) {
        if (vo == null) return null;
        this.setId(vo.getId());
        this.setCognitoSubA(vo.getCognitoSubA());
        this.setCognitoSubB(vo.getCognitoSubB());
        this.setMatchCategory(vo.getMatchCategory());
        this.setCompatibilityScore(Double.valueOf(vo.getCompatibilityScore()));
        this.setScoreBreakdown(vo.getScoreBreakdown());
        this.setStatus(vo.getStatus());
        this.setIsMutual(vo.getIsMutual());
        this.setShownAt(vo.getShownAt());
        this.setActedAt(vo.getActedAt());
        return this;
    }

    public MatchResultVO toVO() {
        MatchResultVO vo = new MatchResultVO();
        vo.setId(this.getId());
        vo.setCognitoSubA(this.getCognitoSubA());
        vo.setCognitoSubB(this.getCognitoSubB());
        vo.setMatchCategory(this.getMatchCategory());
        vo.setCompatibilityScore(this.compatibilityScore);
        vo.setScoreBreakdown(this.getScoreBreakdown());
        vo.setStatus(this.getStatus());
        vo.setIsMutual(this.getIsMutual());
        vo.setShownAt(this.getShownAt());
        vo.setActedAt(this.getActedAt());
        return vo;
    }
}
