package com.shiviishiv7.matchmaking.provider.implementation;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.provider.model.MatchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface MatchResultRepository extends JpaRepository<MatchResult, Integer> {

    Optional<MatchResult> findByCognitoSubAAndCognitoSubBAndMatchCategory(String cognitoSubA, String cognitoSubB, MatchCategory matchCategory);


    List<MatchResult> findByCognitoSubAAndMatchCategoryAndStatus(String cognitoSubA, MatchCategory matchCategory, MatchStatus status);

    List<MatchResult> findByCognitoSubAAndMatchCategory(String cognitoSubA, MatchCategory matchCategory);

    /** All candidateUserIds this user has already seen for a given category */
    @Query("SELECT m.cognitoSubB FROM MatchResult m " +
           "WHERE m.cognitoSubA = :userId AND m.matchCategory = :category")
    Set<String> findSeenCandidateIds(
            @Param("userId") String userId,
            @Param("category") MatchCategory category);

    /** Check if both sides have LIKED each other — used to detect mutual match */
    @Query("SELECT COUNT(m) FROM MatchResult m " +
           "WHERE m.cognitoSubA = :userA AND m.cognitoSubB = :userB " +
           "AND m.matchCategory = :category AND m.status = 'LIKED'")
    long countMutualLike(
            @Param("userA") String userA,
            @Param("userB") String userB,
            @Param("category") MatchCategory category);



    boolean existsByCognitoSubAAndCognitoSubBAndMatchCategory(String cognitoSubA, String cognitoSubB, MatchCategory matchCategory);

}
