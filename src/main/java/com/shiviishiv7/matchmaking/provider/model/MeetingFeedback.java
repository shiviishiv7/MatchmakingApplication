package com.shiviishiv7.matchmaking.provider.model;




import com.shiviishiv7.matchmaking.common.enums.FeedbackResponse;

import jakarta.persistence.*;
import lombok.*;



/**
 * MeetingFeedback — one user's response after one meeting.
 * Each meeting has exactly 2 rows (one per user).
 *
 * Decision matrix:
 *   YES + YES → ANOTHER_ROUND  (engine schedules next meeting)
 *   NO  + *  → ENDED
 */
@Entity
@Table(name = "MEETING_FEEDBACK", indexes = {
        @Index(name = "idx_meeting_feedback_meeting_id", columnList = "meetingId"),
        @Index(name = "idx_meeting_feedback_cognito_sub", columnList = "cognitoSub"),
        @Index(name = "idx_meeting_feedback_meeting_sub", columnList = "meetingId, cognitoSub", unique = true)
})
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
    private FeedbackResponse response;          // YES | NO

    @Column(name = "notes", length = 300)
    private String notes;                       // optional private note (never shown to match)
}
