package com.shiviishiv7.matchmaking.provider.implementation;


import com.shiviishiv7.matchmaking.provider.model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Integer> {

    List<Meeting> findByMatchId(String matchId);

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
        AND m.matchId IN (
            SELECT CAST(ma.id AS string) FROM Match ma
            WHERE ma.cognitoSubA = :sub OR ma.cognitoSubB = :sub
        )
    """)
    List<Meeting> findUpcomingForUser(String sub, LocalDateTime now);
}
