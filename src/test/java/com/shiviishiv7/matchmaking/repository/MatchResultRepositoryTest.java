package com.shiviishiv7.matchmaking.repository;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.provider.implementation.MatchResultRepository;
import com.shiviishiv7.matchmaking.provider.model.MatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the JPQL queries in MatchResultRepository —
 * specifically the countMutualLike fix (was using a string literal 'LIKED'
 * instead of a proper enum param, so it always returned 0).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.hibernate.physical_naming_strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl"
})
class MatchResultRepositoryTest {

    @Autowired
    private MatchResultRepository repo;

    private static final String USER_A = "sub-user-a";
    private static final String USER_B = "sub-user-b";
    private static final MatchCategory CAT = MatchCategory.CASUAL_DATING;

    @BeforeEach
    void setUp() {
        repo.deleteAll();
    }

    // ── countMutualLike ────────────────────────────────────────────────────────

    @Test
    @DisplayName("countMutualLike returns 0 when B has not yet liked A")
    void countMutualLike_noLikeYet_returnsZero() {
        // A liked B, but B hasn't acted yet
        save(USER_A, USER_B, MatchStatus.LIKED);

        long count = repo.countMutualLike(USER_B, USER_A, CAT, MatchStatus.LIKED);
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("countMutualLike returns 1 when B has already liked A — mutual match detected")
    void countMutualLike_bAlreadyLikedA_returnsOne() {
        // B liked A first
        save(USER_B, USER_A, MatchStatus.LIKED);

        // Now A is liking B — check if B already liked A
        long count = repo.countMutualLike(USER_B, USER_A, CAT, MatchStatus.LIKED);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("countMutualLike returns 0 when B SKIPPED A, not liked")
    void countMutualLike_bSkippedA_returnsZero() {
        save(USER_B, USER_A, MatchStatus.SKIPPED);

        long count = repo.countMutualLike(USER_B, USER_A, CAT, MatchStatus.LIKED);
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("countMutualLike is category-scoped — like in different category is not counted")
    void countMutualLike_differentCategory_returnsZero() {
        // B liked A, but for a different category
        save(USER_B, USER_A, MatchStatus.LIKED, MatchCategory.MENTORSHIP);

        long count = repo.countMutualLike(USER_B, USER_A, CAT, MatchStatus.LIKED);
        assertThat(count).isZero();
    }

    // ── findSeenCandidateIds ───────────────────────────────────────────────────

    @Test
    @DisplayName("findSeenCandidateIds returns all candidates A has seen, regardless of status")
    void findSeenCandidates_returnsAll() {
        save(USER_A, USER_B, MatchStatus.PENDING);
        save(USER_A, "sub-user-c", MatchStatus.LIKED);
        save(USER_A, "sub-user-d", MatchStatus.SKIPPED);

        Set<String> seen = repo.findSeenCandidateIds(USER_A, CAT);
        assertThat(seen).containsExactlyInAnyOrder(USER_B, "sub-user-c", "sub-user-d");
    }

    @Test
    @DisplayName("findSeenCandidateIds is scoped per user — other users' results are excluded")
    void findSeenCandidates_otherUserNotIncluded() {
        save(USER_B, "sub-user-c", MatchStatus.LIKED);

        Set<String> seen = repo.findSeenCandidateIds(USER_A, CAT);
        assertThat(seen).isEmpty();
    }

    // ── existsByCognitoSubAAndCognitoSubBAndMatchCategory ─────────────────────

    @Test
    @DisplayName("exists check returns true for an already-persisted pair")
    void exists_persistedPair_returnsTrue() {
        save(USER_A, USER_B, MatchStatus.PENDING);
        assertThat(repo.existsByCognitoSubAAndCognitoSubBAndMatchCategory(USER_A, USER_B, CAT)).isTrue();
    }

    @Test
    @DisplayName("exists check returns false for unknown pair")
    void exists_unknownPair_returnsFalse() {
        assertThat(repo.existsByCognitoSubAAndCognitoSubBAndMatchCategory(USER_A, USER_B, CAT)).isFalse();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void save(String subA, String subB, MatchStatus status) {
        save(subA, subB, status, CAT);
    }

    private void save(String subA, String subB, MatchStatus status, MatchCategory category) {
        repo.save(MatchResult.builder()
                .cognitoSubA(subA)
                .cognitoSubB(subB)
                .matchCategory(category)
                .status(status)
                .isMutual(false)
                .shownAt(LocalDateTime.now())
                .build());
    }
}
