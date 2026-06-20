package com.shiviishiv7.matchmaking.provider.model;




import com.shiviishiv7.matchmaking.common.enums.FeedbackResponse;

import jakarta.persistence.*;
import lombok.*;



/**
 * MeetingFeedback — one user's response after one meeting.
 * Each meeting has exactly 2 feedback rows (one per user).
 * The FeedbackService evaluates both to determine the next state.
 *
 * Decision matrix:
 *   INTERESTED    + INTERESTED    → COMPLETED  (share phone)
 *   ANOTHER_ROUND + ANOTHER_ROUND → ANOTHER_ROUND (new meeting)
 *   INTERESTED    + ANOTHER_ROUND → ANOTHER_ROUND (conservative)
 *   Either        + NOT_INTERESTED → ENDED
 */
@Entity
@Table(name = "meeting_feedback",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_feedback_meeting_user",
                columnNames = {"meeting_id", "user_id"}   // one feedback per user per meeting
        ),
        indexes = {
                @Index(name = "idx_feedback_meeting", columnList = "meeting_id"),
                @Index(name = "idx_feedback_user",    columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingFeedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "meetingId", nullable = false)
    private String meetingId;

    @Column(name = "cognitoSub", nullable = false)
    private String cognitoSub;

    @Enumerated(EnumType.STRING)
    @Column(name = "response", nullable = false, length = 20)
    private FeedbackResponse response;          // INTERESTED | ANOTHER_ROUND | NOT_INTERESTED

    @Column(name = "notes", length = 300)
    private String notes;                       // optional private note (never shown to match)
}
