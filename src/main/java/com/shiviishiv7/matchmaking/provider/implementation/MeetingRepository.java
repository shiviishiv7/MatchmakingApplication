package com.shiviishiv7.matchmaking.provider.implementation;


import com.shiviishiv7.matchmaking.provider.model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Integer> {

    List<Meeting> findByMatchResultId(Integer matchResultId);

    // SCHEDULED meetings whose scheduledAt has arrived — ready to open the waiting room
    @Query("""
        SELECT m FROM Meeting m
        WHERE m.status = 'SCHEDULED'
        AND m.scheduledAt <= :now
    """)
    List<Meeting> findReadyToOpenWaitingRoom(LocalDateTime now);

    // WAITING_ROOM or IN_PROGRESS meetings whose duration has elapsed — ready to complete
    @Query("""
        SELECT m FROM Meeting m
        WHERE m.status IN ('WAITING_ROOM', 'IN_PROGRESS')
        AND m.scheduledAt <= :now
    """)
    List<Meeting> findExpiredActiveMeetings(LocalDateTime now);

    // Upcoming SCHEDULED meetings for a user (as either party in the match)
    @Query("""
        SELECT m FROM Meeting m
        WHERE m.status = 'SCHEDULED'
        AND m.scheduledAt > :now
        AND m.matchResultId IN (
            SELECT mr.id FROM MatchResult mr
            WHERE mr.cognitoSubA = :sub OR mr.cognitoSubB = :sub
        )
    """)
    List<Meeting> findUpcomingForUser(String sub, LocalDateTime now);

    /** Meetings created today where at least one email is still pending */
    @Query("""
        SELECT m FROM Meeting m
        WHERE m.createdAt >= :startOfDay
        AND m.createdAt < :endOfDay
        AND (m.emailSentA = false OR m.emailSentB = false)
    """)
    List<Meeting> findTodayMeetingsWithPendingEmails(
        @org.springframework.data.repository.query.Param("startOfDay") LocalDateTime startOfDay,
        @org.springframework.data.repository.query.Param("endOfDay") LocalDateTime endOfDay);

    /** Check if user already has a meeting scheduled today */
    @Query("""
        SELECT COUNT(m) > 0 FROM Meeting m
        WHERE m.status = 'SCHEDULED'
        AND m.scheduledAt >= :startOfDay
        AND m.scheduledAt < :endOfDay
        AND m.matchResultId IN (
            SELECT mr.id FROM MatchResult mr
            WHERE mr.cognitoSubA = :sub OR mr.cognitoSubB = :sub
        )
    """)
    boolean hasScheduledMeetingToday(
        @org.springframework.data.repository.query.Param("sub") String sub,
        @org.springframework.data.repository.query.Param("startOfDay") LocalDateTime startOfDay,
        @org.springframework.data.repository.query.Param("endOfDay") LocalDateTime endOfDay);
}
