package com.shiviishiv7.matchmaking.provider.model;

import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingType;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Match — a pairing between two users created by the matching engine.
 * One Match can span multiple Meetings (rounds).
 * roundCount tracks how many meetings have happened.
 * maxRounds caps the loop to prevent infinite recurring meetings.
 */
@Entity
@Table(name = "matches", indexes = {
        @Index(name = "idx_match_user_a", columnList = "user_a_id"),
        @Index(name = "idx_match_user_b", columnList = "user_b_id"),
        @Index(name = "idx_match_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // ── The two matched users ─────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_a_id", nullable = false)
    private User userA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_b_id", nullable = false)
    private User userB;

    // ── Match lifecycle ───────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private MatchStatus status = MatchStatus.PENDING;

    @Column(name = "compatibility_score")
    private Double compatibilityScore;          // 0.0 to 1.0, computed at match time

    // ── How this match was initiated ──────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_type", nullable = false, length = 20)
    @Builder.Default
    private MeetingType meetingType = MeetingType.SCHEDULED;

    // ── Round tracking ────────────────────────────────────────────────────
    @Column(name = "round_count", nullable = false)
    @Builder.Default
    private Integer roundCount = 0;             // increments after each meeting

    @Column(name = "max_rounds", nullable = false)
    @Builder.Default
    private Integer maxRounds = 3;              // from application.yml zoom.max-rounds

    // ── Relationships ─────────────────────────────────────────────────────
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("scheduledAt ASC")
    private List<Meeting> meetings;
}
