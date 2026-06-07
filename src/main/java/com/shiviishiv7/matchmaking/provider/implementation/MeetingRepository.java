package com.shiviishiv7.matchmaking.provider.implementation;


import com.shiviishiv7.matchmaking.provider.model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, UUID> {

    List<Meeting> findByMatchId(UUID matchId);

    // Find meetings that have ended but not yet marked COMPLETED
    // Used by scheduler to trigger feedback requests
    @Query("""
        SELECT m FROM Meeting m
        WHERE m.status = 'SCHEDULED'
        AND m.scheduledAt < :cutoff
    """)
    List<Meeting> findCompletedButNotMarked(LocalDateTime cutoff);
}
