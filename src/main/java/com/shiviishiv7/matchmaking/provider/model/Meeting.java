package com.shiviishiv7.matchmaking.provider.model;




import com.shiviishiv7.matchmaking.common.enums.MeetingStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;


/**
 * Meeting — one WebRTC meeting within a Match.
 * A Match with 3 rounds will have 3 Meeting rows,
 * each with its own scheduling and feedback entries.
 */
@Entity
@Table(name = "MEETING", indexes = {
        @Index(name = "idx_meeting_match_result_id", columnList = "matchResultId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meeting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    // ── Round info ────────────────────────────────────────────────────────
    @Column(name = "matchResultId", nullable = false)
    private Integer matchResultId;
    @Column(name = "roundNumber", nullable = false)
    private Integer roundNumber;                // 1, 2, 3 ...

    // ── Scheduling ────────────────────────────────────────────────────────
    @Column(name = "scheduledAt", nullable = true)
    private LocalDateTime scheduledAt;

    @Column(name = "durationMinutes", nullable = false)
    @Builder.Default
    private Integer durationMinutes = 30;

    // ── Meeting type ──────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "meetingType", nullable = false, length = 20)
    @Builder.Default
    private MeetingType meetingType = MeetingType.SCHEDULED;

    // ── Status ────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private MeetingStatus status = MeetingStatus.SCHEDULED;

    // ── Zoom ──────────────────────────────────────────────────────────────
    @Column(name = "zoomMeetingId", length = 50)
    private String zoomMeetingId;

    @Column(name = "zoomJoinUrl", length = 500)
    private String zoomJoinUrl;

    @Column(name = "zoomStartUrl", length = 1000)
    private String zoomStartUrl;

    @Column(name = "zoomPassword", length = 50)
    private String zoomPassword;

}
