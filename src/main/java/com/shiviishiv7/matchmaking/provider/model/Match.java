package com.shiviishiv7.matchmaking.provider.model;

import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingType;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;


/**
 * Match — a pairing between two users created by the matching engine.
 * One Match can span multiple Meetings (rounds).
 * roundCount tracks how many meetings have happened.
 * maxRounds caps the loop to prevent infinite recurring meetings.
 */
@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    // ── The two matched users ─────────────────────────────────────────────
    @Column(name = "cognitoSubA", nullable = false)
    private String cognitoSubA;

    @Column(name = "cognitoSubB", nullable = false)
    private String cognitoSubB;

    // ── Match lifecycle ───────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private MatchStatus status = MatchStatus.PENDING;

    @Column(name = "compatibilityScore")
    private Double compatibilityScore;          // 0.0 to 1.0, computed at match time

    // ── How this match was initiated ──────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "meetingType", nullable = false, length = 20)
    @Builder.Default
    private MeetingType meetingType = MeetingType.SCHEDULED;

    // ── Round tracking ────────────────────────────────────────────────────
    @Column(name = "roundCount", nullable = false)
    @Builder.Default
    private Integer roundCount = 0;             // increments after each meeting

    @Column(name = "maxRounds", nullable = false)
    @Builder.Default
    private Integer maxRounds = 3;              // from application.yml zoom.max-rounds

//    // ── Relationships ─────────────────────────────────────────────────────
//    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @OrderBy("scheduledAt ASC")
//    private List<Meeting> meetings;
}
