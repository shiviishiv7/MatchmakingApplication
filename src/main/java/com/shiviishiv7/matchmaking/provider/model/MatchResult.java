package com.shiviishiv7.matchmaking.provider.model;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.provider.vo.MatchResultVO;
import jakarta.persistence.*;
import lombok.*;

/**
 * One engine-assigned match between two users.
 * Created by the matching engine; progresses through rounds of meetings
 * until one user says NO (ENDED) or all rounds complete (COMPLETED).
 */
@Entity
@Table(name = "MATCH_RESULT", indexes = {
        @Index(name = "idx_match_result_sub_a", columnList = "cognitoSubA"),
        @Index(name = "idx_match_result_sub_b", columnList = "cognitoSubB"),
        @Index(name = "idx_match_result_sub_a_category", columnList = "cognitoSubA, matchCategory")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MatchResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "cognitoSubA", nullable = false)
    private String cognitoSubA;

    @Column(name = "cognitoSubB", nullable = false)
    private String cognitoSubB;

    @Enumerated(EnumType.STRING)
    @Column(name = "matchCategory", nullable = false, length = 60)
    private MatchCategory matchCategory;

    @Column(name = "compatibilityScore")
    private Double compatibilityScore;

    @Column(name = "scoreBreakdown", columnDefinition = "JSON")
    private String scoreBreakdown;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private MatchStatus status = MatchStatus.PENDING;

    @Column(name = "roundCount")
    @Builder.Default
    private Integer roundCount = 0;

    @Column(name = "maxRounds")
    @Builder.Default
    private Integer maxRounds = 3;

    public MatchResult fromVO(MatchResultVO vo) {
        if (vo == null) return null;
        this.setId(vo.getId());
        this.setCognitoSubA(vo.getCognitoSubA());
        this.setCognitoSubB(vo.getCognitoSubB());
        this.setMatchCategory(vo.getMatchCategory());
        this.setCompatibilityScore(vo.getCompatibilityScore());
        this.setScoreBreakdown(vo.getScoreBreakdown());
        this.setStatus(vo.getStatus());
        this.setRoundCount(vo.getRoundCount());
        this.setMaxRounds(vo.getMaxRounds());
        return this;
    }

    public MatchResultVO toVO() {
        MatchResultVO vo = new MatchResultVO();
        vo.setId(this.getId());
        vo.setCognitoSubA(this.getCognitoSubA());
        vo.setCognitoSubB(this.getCognitoSubB());
        vo.setMatchCategory(this.getMatchCategory());
        vo.setCompatibilityScore(this.getCompatibilityScore());
        vo.setScoreBreakdown(this.getScoreBreakdown());
        vo.setStatus(this.getStatus());
        vo.setRoundCount(this.getRoundCount());
        vo.setMaxRounds(this.getMaxRounds());
        return vo;
    }
}
