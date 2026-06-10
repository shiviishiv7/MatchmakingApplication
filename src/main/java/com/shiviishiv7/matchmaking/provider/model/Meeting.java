package com.shiviishiv7.matchmaking.provider.model;




import com.shiviishiv7.matchmaking.common.enums.MeetingStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Meeting — one WebRTC meeting within a Match.
 * A Match with 3 rounds will have 3 Meeting rows,
 * each with its own scheduling and feedback entries.
 */
@Entity
@Table(name = "meetings", indexes = {
        @Index(name = "idx_meeting_match",  columnList = "match_id"),
        @Index(name = "idx_meeting_status", columnList = "status"),
        @Index(name = "idx_meeting_time",   columnList = "scheduled_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meeting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    // ── Round info ────────────────────────────────────────────────────────
    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;                // 1, 2, 3 ...

    // ── Scheduling ────────────────────────────────────────────────────────
    @Column(name = "scheduled_at", nullable = true)
    private LocalDateTime scheduledAt;

    @Column(name = "duration_minutes", nullable = false)
    @Builder.Default
    private Integer durationMinutes = 30;

    // ── Meeting type ──────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_type", nullable = false, length = 20)
    @Builder.Default
    private MeetingType meetingType = MeetingType.SCHEDULED;

    // ── Status ────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private MeetingStatus status = MeetingStatus.SCHEDULED;

    // ── Feedback ─────────────────────────────────────────────────────────
    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MeetingFeedback> feedbacks;
}
