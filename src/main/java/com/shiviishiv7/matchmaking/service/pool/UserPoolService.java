package com.shiviishiv7.matchmaking.service.pool;

import com.shiviishiv7.matchmaking.provider.vo.ws.InstantSearchFilterVO;
import com.shiviishiv7.matchmaking.provider.vo.ws.PoolUserVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Maintains the in-memory pool of users currently available for WebRTC connection.
 *
 * Key   = cognitoSub
 * Value = PoolUserVO (display + filter info)
 *
 * Also tracks:
 *  - busySubs:        who is currently in an active call
 *  - seenBySub:       which pool users each session has already been shown (dedup)
 *  - pendingRequests: users who searched with a filter and found no one; notified when a match joins
 */
@Service
public class UserPoolService {

    private final Map<String, PoolUserVO>              pool            = new ConcurrentHashMap<>();
    private final Set<String>                          busySubs        = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<String>>             seenBySub       = new ConcurrentHashMap<>();
    private final Map<String, InstantSearchFilterVO>   pendingRequests = new ConcurrentHashMap<>();

    // ── Pool management ────────────────────────────────────────────────────────

    /** Adds user to the pool. No-op if already present. Returns false if skipped. */
    public boolean addUser(PoolUserVO user) {
        if (pool.containsKey(user.getCognitoSub())) return false;
        pool.put(user.getCognitoSub(), user);
        return true;
    }

    /** Removes user from pool, clears all their session state. */
    public void removeUser(String cognitoSub) {
        pool.remove(cognitoSub);
        busySubs.remove(cognitoSub);
        seenBySub.remove(cognitoSub);
        pendingRequests.remove(cognitoSub);
    }

    public boolean isInPool(String cognitoSub) {
        return pool.containsKey(cognitoSub);
    }

    // ── Busy state ─────────────────────────────────────────────────────────────

    public void markBusy(String cognitoSub)      { busySubs.add(cognitoSub); }
    public void markAvailable(String cognitoSub) { busySubs.remove(cognitoSub); }
    public boolean isBusy(String cognitoSub)     { return busySubs.contains(cognitoSub); }

    // ── Seen-users dedup ───────────────────────────────────────────────────────

    /** Mark a list of pool users as seen by this requester so they aren't shown again. */
    public void markAsSeen(String requesterSub, List<PoolUserVO> users) {
        Set<String> seen = seenBySub.computeIfAbsent(requesterSub, k -> ConcurrentHashMap.newKeySet());
        users.forEach(u -> seen.add(u.getCognitoSub()));
    }

    private Set<String> getSeenSubs(String requesterSub) {
        return seenBySub.getOrDefault(requesterSub, Collections.emptySet());
    }

    // ── No-filter pool list (used at join time) ────────────────────────────────

    /**
     * Returns all pool users except the requester and anyone already seen this session.
     */
    public List<PoolUserVO> getOtherUsers(String requesterSub) {
        Set<String> seen = getSeenSubs(requesterSub);
        return pool.values().stream()
                .filter(u -> !u.getCognitoSub().equals(requesterSub))
                .filter(u -> !seen.contains(u.getCognitoSub()))
                .collect(Collectors.toList());
    }

    // ── Filtered search ────────────────────────────────────────────────────────

    /**
     * Returns pool users matching the given filter, excluding self and already-seen.
     * childCategory present → advanced (category + basic fields).
     * childCategory absent  → basic (gender + city + age).
     */
    public List<PoolUserVO> getFilteredUsers(String requesterSub, InstantSearchFilterVO filter) {
        Set<String> seen = getSeenSubs(requesterSub);
        return pool.values().stream()
                .filter(u -> !u.getCognitoSub().equals(requesterSub))
                .filter(u -> !seen.contains(u.getCognitoSub()))
                .filter(u -> matchesBasicFilter(u, filter))
                .filter(u -> filter.getChildCategory() == null || matchesCategory(u, filter.getChildCategory()))
                .collect(Collectors.toList());
    }

    private boolean matchesBasicFilter(PoolUserVO user, InstantSearchFilterVO filter) {
        if (filter.getPreferredGender() != null && !filter.getPreferredGender().isBlank()
                && !filter.getPreferredGender().equalsIgnoreCase("ANY")) {
            if (user.getGender() == null || !user.getGender().equalsIgnoreCase(filter.getPreferredGender())) {
                return false;
            }
        }
        if (filter.getPreferredCity() != null && !filter.getPreferredCity().isBlank()) {
            if (user.getCurrentCity() == null || !user.getCurrentCity().equalsIgnoreCase(filter.getPreferredCity())) {
                return false;
            }
        }
        if (filter.getMinAge() != null || filter.getMaxAge() != null) {
            if (user.getDateOfBirth() == null) return false;
            int age = Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();
            if (filter.getMinAge() != null && age < filter.getMinAge()) return false;
            if (filter.getMaxAge() != null && age > filter.getMaxAge()) return false;
        }
        return true;
    }

    private boolean matchesCategory(PoolUserVO user, String childCategory) {
        if (user.getMatchCategories() == null || user.getMatchCategories().isEmpty()) return false;
        return user.getMatchCategories().stream()
                .anyMatch(cat -> cat.equalsIgnoreCase(childCategory));
    }

    // ── Pending requests ───────────────────────────────────────────────────────

    /** Save a filter request for a user who found no instant match. */
    public void addPendingRequest(String cognitoSub, InstantSearchFilterVO filter) {
        pendingRequests.put(cognitoSub, filter);
    }

    public void removePendingRequest(String cognitoSub) {
        pendingRequests.remove(cognitoSub);
    }

    /**
     * When a new user joins the pool, check if they satisfy any pending filter requests.
     * Returns a map of requesterSub → newJoiner for all matches found.
     * Removes matched pending requests so each requester is notified once.
     */
    public Map<String, PoolUserVO> findPendingMatches(PoolUserVO newJoiner) {
        Map<String, PoolUserVO> matches = new HashMap<>();
        Iterator<Map.Entry<String, InstantSearchFilterVO>> it = pendingRequests.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, InstantSearchFilterVO> entry = it.next();
            String requesterSub = entry.getKey();
            InstantSearchFilterVO filter = entry.getValue();
            boolean satisfiesBasic = matchesBasicFilter(newJoiner, filter);
            boolean satisfiesCategory = filter.getChildCategory() == null || matchesCategory(newJoiner, filter.getChildCategory());
            if (satisfiesBasic && satisfiesCategory) {
                matches.put(requesterSub, newJoiner);
                it.remove(); // notify once
            }
        }
        return matches;
    }
}
