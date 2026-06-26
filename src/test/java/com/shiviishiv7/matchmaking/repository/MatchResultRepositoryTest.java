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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

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

    // ── findSeenCandidateIds ───────────────────────────────────────────────────

    @Test
    @DisplayName("findSeenCandidateIds returns all candidates A has been matched with")
    void findSeenCandidates_returnsAll() {
        save(USER_A, USER_B, MatchStatus.MEETING_SCHEDULED);
        save(USER_A, "sub-user-c", MatchStatus.ANOTHER_ROUND);
        save(USER_A, "sub-user-d", MatchStatus.ENDED);

        Set<String> seen = repo.findSeenCandidateIds(USER_A, CAT);
        assertThat(seen).containsExactlyInAnyOrder(USER_B, "sub-user-c", "sub-user-d");
    }

    @Test
    @DisplayName("findSeenCandidateIds is scoped per user — other users' results are excluded")
    void findSeenCandidates_otherUserNotIncluded() {
        save(USER_B, "sub-user-c", MatchStatus.MEETING_SCHEDULED);

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

    @Test
    @DisplayName("exists check is category-scoped — different category returns false")
    void exists_differentCategory_returnsFalse() {
        save(USER_A, USER_B, MatchStatus.PENDING, MatchCategory.MENTORSHIP);
        assertThat(repo.existsByCognitoSubAAndCognitoSubBAndMatchCategory(USER_A, USER_B, CAT)).isFalse();
    }

    // ── findByStatus ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByStatus returns only matches in the given status")
    void findByStatus_returnsMatchingRows() {
        save(USER_A, USER_B, MatchStatus.AWAITING_FEEDBACK);
        save(USER_A, "sub-user-c", MatchStatus.ENDED);

        assertThat(repo.findByStatus(MatchStatus.AWAITING_FEEDBACK)).hasSize(1);
        assertThat(repo.findByStatus(MatchStatus.ENDED)).hasSize(1);
        assertThat(repo.findByStatus(MatchStatus.PENDING)).isEmpty();
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
                .build());
    }
}
