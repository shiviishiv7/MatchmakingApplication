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

    Optional<MatchResult> findByUserIdAndCandidateUserIdAndMatchCategory(
            Integer userId, Integer candidateUserId, MatchCategory matchCategory);

    List<MatchResult> findByUserIdAndMatchCategoryAndStatus(
            Integer userId, MatchCategory matchCategory, MatchStatus status);

    List<MatchResult> findByUserIdAndMatchCategory(Integer userId, MatchCategory matchCategory);

    /** All candidateUserIds this user has already seen for a given category */
    @Query("SELECT m.candidateUserId FROM MatchResult m " +
           "WHERE m.userId = :userId AND m.matchCategory = :category")
    Set<Integer> findSeenCandidateIds(
            @Param("userId") Integer userId,
            @Param("category") MatchCategory category);

    /** Check if both sides have LIKED each other — used to detect mutual match */
    @Query("SELECT COUNT(m) FROM MatchResult m " +
           "WHERE m.userId = :userA AND m.candidateUserId = :userB " +
           "AND m.matchCategory = :category AND m.status = 'LIKED'")
    long countMutualLike(
            @Param("userA") Integer userA,
            @Param("userB") Integer userB,
            @Param("category") MatchCategory category);

    boolean existsByUserIdAndCandidateUserIdAndMatchCategory(
            Integer userId, Integer candidateUserId, MatchCategory matchCategory);
}
