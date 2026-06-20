package com.shiviishiv7.matchmaking.provider.implementation;


import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.provider.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface MatchRepository extends JpaRepository<Match, Integer> {

    List<Match> findByStatus(MatchStatus status);

    Optional<Match> findMatchByCognitoSubA(String cognitoSubA);

    Optional<Match> findMatchByCognitoSubAOrCognitoSubB(String cognitoSubA, String cognitoSubB);

    boolean existsByCognitoSubAAndCognitoSubB(String cognitoSubA, String cognitoSubB);
}
