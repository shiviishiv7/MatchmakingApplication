package com.shiviishiv7.matchmaking.pool;

import com.shiviishiv7.matchmaking.provider.vo.ws.InstantSearchFilterVO;
import com.shiviishiv7.matchmaking.provider.vo.ws.PoolUserVO;
import com.shiviishiv7.matchmaking.service.pool.UserPoolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UserPoolService — covers:
 *  - Pool management (add/remove/dedup)
 *  - Seen-users dedup
 *  - Basic filter  (gender + city + age)
 *  - Advanced filter (basic + category)
 *  - Pending request matching (notified when a compatible user joins)
 *  - Busy state
 */
class UserPoolServiceTest {

    private UserPoolService service;

    // Fixed reference date so age calculations are deterministic
    // "now" inside the service uses LocalDate.now(); we build DOBs relative to today
    private static final LocalDate TODAY = LocalDate.now();

    @BeforeEach
    void setUp() {
        service = new UserPoolService();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pool management
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-01: addUser returns true and user appears in pool")
    void addUser_newUser_returnsTrue() {
        boolean added = service.addUser(poolUser("alice", "FEMALE", "Mumbai", 25, List.of()));
        assertThat(added).isTrue();
        assertThat(service.isInPool("alice")).isTrue();
    }

    @Test
    @DisplayName("TC-02: addUser is idempotent — second call for same sub returns false")
    void addUser_duplicate_returnsFalse() {
        service.addUser(poolUser("alice", "FEMALE", "Mumbai", 25, List.of()));
        boolean second = service.addUser(poolUser("alice", "FEMALE", "Mumbai", 25, List.of()));
        assertThat(second).isFalse();
    }

    @Test
    @DisplayName("TC-03: removeUser removes user and clears busy + seen + pending state")
    void removeUser_clearsAllState() {
        service.addUser(poolUser("alice", "FEMALE", "Mumbai", 25, List.of()));
        service.markBusy("alice");
        service.addPendingRequest("alice", new InstantSearchFilterVO());

        service.removeUser("alice");

        assertThat(service.isInPool("alice")).isFalse();
        assertThat(service.isBusy("alice")).isFalse();
        // pending request also cleared — new joiner should find no pending for alice
        assertThat(service.findPendingMatches(poolUser("bob", "MALE", "Delhi", 28, List.of()))).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Seen-users dedup
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-04: getOtherUsers excludes the requester themselves")
    void getOtherUsers_excludesSelf() {
        service.addUser(poolUser("alice", "FEMALE", "Mumbai", 25, List.of()));
        service.addUser(poolUser("bob",   "MALE",   "Delhi",  28, List.of()));

        List<PoolUserVO> result = service.getOtherUsers("alice");
        assertThat(result).extracting(PoolUserVO::getCognitoSub).containsOnly("bob");
    }

    @Test
    @DisplayName("TC-05: getOtherUsers excludes users already marked as seen")
    void getOtherUsers_excludesSeen() {
        service.addUser(poolUser("alice", "FEMALE", "Mumbai", 25, List.of()));
        service.addUser(poolUser("bob",   "MALE",   "Delhi",  28, List.of()));
        service.addUser(poolUser("carol", "FEMALE", "Mumbai", 30, List.of()));

        // alice already saw bob
        service.markAsSeen("alice", List.of(poolUser("bob", "MALE", "Delhi", 28, List.of())));

        List<PoolUserVO> result = service.getOtherUsers("alice");
        assertThat(result).extracting(PoolUserVO::getCognitoSub).containsOnly("carol");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Basic filter — gender
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-06: basic filter — gender match returns only matching gender")
    void basicFilter_genderMatch_returnsOnlyMatchingUsers() {
        service.addUser(poolUser("alice", "FEMALE", "Mumbai", 25, List.of()));
        service.addUser(poolUser("bob",   "MALE",   "Mumbai", 25, List.of()));

        InstantSearchFilterVO filter = filter(null, "FEMALE", null, null, null);
        List<PoolUserVO> result = service.getFilteredUsers("requester", filter);

        assertThat(result).extracting(PoolUserVO::getCognitoSub).containsOnly("alice");
    }

    @Test
    @DisplayName("TC-07: basic filter — gender ANY returns everyone")
    void basicFilter_genderAny_returnsAll() {
        service.addUser(poolUser("alice", "FEMALE", "Mumbai", 25, List.of()));
        service.addUser(poolUser("bob",   "MALE",   "Mumbai", 25, List.of()));

        InstantSearchFilterVO filter = filter(null, "ANY", null, null, null);
        List<PoolUserVO> result = service.getFilteredUsers("requester", filter);

        assertThat(result).hasSize(2);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Basic filter — city
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-08: basic filter — city match is case-insensitive")
    void basicFilter_cityMatch_caseInsensitive() {
        service.addUser(poolUser("alice", "FEMALE", "mumbai", 25, List.of()));
        service.addUser(poolUser("bob",   "MALE",   "Delhi",  25, List.of()));

        InstantSearchFilterVO filter = filter(null, null, "MUMBAI", null, null);
        List<PoolUserVO> result = service.getFilteredUsers("requester", filter);

        assertThat(result).extracting(PoolUserVO::getCognitoSub).containsOnly("alice");
    }

    @Test
    @DisplayName("TC-09: basic filter — different city returns empty")
    void basicFilter_cityMismatch_returnsEmpty() {
        service.addUser(poolUser("alice", "FEMALE", "Pune", 25, List.of()));

        InstantSearchFilterVO filter = filter(null, null, "Mumbai", null, null);
        List<PoolUserVO> result = service.getFilteredUsers("requester", filter);

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Basic filter — age range
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-10: basic filter — user within age range is included")
    void basicFilter_ageInRange_included() {
        service.addUser(poolUser("alice", "FEMALE", "Mumbai", 28, List.of()));

        InstantSearchFilterVO filter = filter(null, null, null, 25, 35);
        List<PoolUserVO> result = service.getFilteredUsers("requester", filter);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("TC-11: basic filter — user outside age range is excluded")
    void basicFilter_ageOutOfRange_excluded() {
        service.addUser(poolUser("alice", "FEMALE", "Mumbai", 40, List.of()));

        InstantSearchFilterVO filter = filter(null, null, null, 25, 35);
        List<PoolUserVO> result = service.getFilteredUsers("requester", filter);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("TC-12: basic filter — user with null dateOfBirth excluded when age filter is active")
    void basicFilter_nullDob_excludedWhenAgeFilterActive() {
        PoolUserVO noDob = PoolUserVO.builder()
                .cognitoSub("nodob").gender("FEMALE").currentCity("Mumbai")
                .dateOfBirth(null).matchCategories(List.of()).build();
        service.addUser(noDob);

        InstantSearchFilterVO filter = filter(null, null, null, 20, 40);
        List<PoolUserVO> result = service.getFilteredUsers("requester", filter);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("TC-13: basic filter — combined gender + city + age, only exact match returned")
    void basicFilter_combined_onlyExactMatchReturned() {
        service.addUser(poolUser("alice", "FEMALE", "Mumbai", 28, List.of())); // matches all
        service.addUser(poolUser("bob",   "MALE",   "Mumbai", 28, List.of())); // wrong gender
        service.addUser(poolUser("carol", "FEMALE", "Delhi",  28, List.of())); // wrong city
        service.addUser(poolUser("dave",  "FEMALE", "Mumbai", 45, List.of())); // too old

        InstantSearchFilterVO filter = filter(null, "FEMALE", "Mumbai", 20, 35);
        List<PoolUserVO> result = service.getFilteredUsers("requester", filter);

        assertThat(result).extracting(PoolUserVO::getCognitoSub).containsOnly("alice");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Advanced filter — category
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-14: advanced filter — user with matching category is included")
    void advancedFilter_categoryMatch_included() {
        service.addUser(poolUser("alice", "FEMALE", "Mumbai", 28, List.of("MENTORSHIP", "GYM_PARTNER")));
        service.addUser(poolUser("bob",   "MALE",   "Mumbai", 28, List.of("CASUAL_DATING")));

        InstantSearchFilterVO filter = filter("MENTORSHIP", null, null, null, null);
        List<PoolUserVO> result = service.getFilteredUsers("requester", filter);

        assertThat(result).extracting(PoolUserVO::getCognitoSub).containsOnly("alice");
    }

    @Test
    @DisplayName("TC-15: advanced filter — category check is case-insensitive")
    void advancedFilter_categoryMatch_caseInsensitive() {
        service.addUser(poolUser("alice", "FEMALE", "Mumbai", 28, List.of("mentorship")));

        InstantSearchFilterVO filter = filter("MENTORSHIP", null, null, null, null);
        List<PoolUserVO> result = service.getFilteredUsers("requester", filter);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("TC-16: advanced filter — user with empty category list is excluded")
    void advancedFilter_emptyCategories_excluded() {
        service.addUser(poolUser("alice", "FEMALE", "Mumbai", 28, List.of()));

        InstantSearchFilterVO filter = filter("MENTORSHIP", null, null, null, null);
        List<PoolUserVO> result = service.getFilteredUsers("requester", filter);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("TC-17: advanced filter — category + basic fields all must pass")
    void advancedFilter_categoryPlusBasic_allMustPass() {
        service.addUser(poolUser("alice", "FEMALE", "Mumbai", 28, List.of("MENTORSHIP"))); // all pass
        service.addUser(poolUser("bob",   "MALE",   "Mumbai", 28, List.of("MENTORSHIP"))); // wrong gender
        service.addUser(poolUser("carol", "FEMALE", "Mumbai", 28, List.of("GYM_PARTNER"))); // wrong category

        InstantSearchFilterVO filter = filter("MENTORSHIP", "FEMALE", "Mumbai", 20, 35);
        List<PoolUserVO> result = service.getFilteredUsers("requester", filter);

        assertThat(result).extracting(PoolUserVO::getCognitoSub).containsOnly("alice");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pending requests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-18: findPendingMatches notifies requester when new joiner matches their basic filter")
    void pendingRequest_basicFilter_notifiesOnMatch() {
        InstantSearchFilterVO pendingFilter = filter(null, "FEMALE", "Mumbai", 20, 35);
        service.addPendingRequest("requester-sub", pendingFilter);

        PoolUserVO newJoiner = poolUser("alice", "FEMALE", "Mumbai", 28, List.of());
        Map<String, PoolUserVO> matches = service.findPendingMatches(newJoiner);

        assertThat(matches).containsKey("requester-sub");
        assertThat(matches.get("requester-sub").getCognitoSub()).isEqualTo("alice");
    }

    @Test
    @DisplayName("TC-19: findPendingMatches removes the pending request after notifying (notify once)")
    void pendingRequest_removedAfterFirstMatch() {
        service.addPendingRequest("requester-sub", filter(null, "FEMALE", "Mumbai", 20, 35));
        PoolUserVO joiner = poolUser("alice", "FEMALE", "Mumbai", 28, List.of());

        service.findPendingMatches(joiner); // first match — removes entry
        Map<String, PoolUserVO> second = service.findPendingMatches(joiner);

        assertThat(second).isEmpty(); // not notified again
    }

    @Test
    @DisplayName("TC-20: findPendingMatches does NOT notify when new joiner doesn't match filter")
    void pendingRequest_noMatch_notNotified() {
        service.addPendingRequest("requester-sub", filter(null, "FEMALE", "Mumbai", 20, 35));

        PoolUserVO mismatch = poolUser("bob", "MALE", "Delhi", 50, List.of()); // wrong on all counts
        Map<String, PoolUserVO> matches = service.findPendingMatches(mismatch);

        assertThat(matches).isEmpty();
    }

    @Test
    @DisplayName("TC-21: findPendingMatches with advanced filter — notifies only when category also matches")
    void pendingRequest_advancedFilter_categoryMustMatch() {
        service.addPendingRequest("requester-sub", filter("MENTORSHIP", "FEMALE", "Mumbai", 20, 35));

        PoolUserVO noCategory = poolUser("alice", "FEMALE", "Mumbai", 28, List.of("GYM_PARTNER"));
        PoolUserVO withCategory = poolUser("carol", "FEMALE", "Mumbai", 28, List.of("MENTORSHIP"));

        assertThat(service.findPendingMatches(noCategory)).isEmpty();
        assertThat(service.findPendingMatches(withCategory)).containsKey("requester-sub");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Busy state
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-22: markBusy / markAvailable / isBusy round-trip")
    void busyState_roundTrip() {
        service.addUser(poolUser("alice", "FEMALE", "Mumbai", 25, List.of()));

        assertThat(service.isBusy("alice")).isFalse();
        service.markBusy("alice");
        assertThat(service.isBusy("alice")).isTrue();
        service.markAvailable("alice");
        assertThat(service.isBusy("alice")).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private PoolUserVO poolUser(String sub, String gender, String city, int age, List<String> categories) {
        return PoolUserVO.builder()
                .cognitoSub(sub)
                .firstName(sub)
                .lastName("")
                .gender(gender)
                .currentCity(city)
                .dateOfBirth(TODAY.minusYears(age))
                .matchCategories(categories)
                .build();
    }

    /** Convenience builder for InstantSearchFilterVO. */
    private InstantSearchFilterVO filter(String category, String gender, String city,
                                         Integer minAge, Integer maxAge) {
        InstantSearchFilterVO f = new InstantSearchFilterVO();
        f.setChildCategory(category);
        f.setPreferredGender(gender);
        f.setPreferredCity(city);
        f.setMinAge(minAge);
        f.setMaxAge(maxAge);
        return f;
    }
}
