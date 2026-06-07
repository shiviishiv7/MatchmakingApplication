package com.shiviishiv7.matchmaking.provider.implementation;


import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.provider.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {

    List<Match> findByStatus(MatchStatus status);

    // Find active match for a user (either side)
    @Query("""
        SELECT m FROM Match m
        WHERE (m.userA.id = :userId OR m.userB.id = :userId)
        AND m.status NOT IN ('COMPLETED', 'ENDED')
        ORDER BY m.createdAt DESC
    """)
    Optional<Match> findActiveMatchForUser(UUID userId);

    // Check if two users have ever been matched
    @Query("""
        SELECT COUNT(m) > 0 FROM Match m
        WHERE (m.userA.id = :userAId AND m.userB.id = :userBId)
           OR (m.userA.id = :userBId AND m.userB.id = :userAId)
    """)
    boolean existsMatchBetween(UUID userAId, UUID userBId);
}
