package com.shiviishiv7.matchmaking.processor.matchingengine;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.provider.vo.MatchCandidateVO;

import java.util.List;

/**
 * Strategy interface — one implementation per MatchCategory.
 * Each scorer knows:
 *   1. Which category it handles (supports())
 *   2. How to build the hard-filter candidate list (fetchCandidates())
 *   3. How to score each candidate (score())
 *   4. How to build the discovery card snippet (buildSnippet())
 */
public interface CategoryScorer {

    MatchCategory supports();

    /**
     * Phase 1: fetch all candidate userIds that pass hard filters for this user.
     * Implementations run a JPQL/native query with WHERE clauses on:
     *   - same category active in registry
     *   - gender preference match
     *   - age range overlap
     *   - location (country/state) if preference set
     * Returns raw userIds — scoring happens next.
     */
    List<String> fetchCandidateIds(String userId, List<String> excludeIds);

    /**
     * Phase 2: score a single candidate against the requesting user.
     * Populates candidateVO.compatibilityScore and candidateVO.scoreBreakdown.
     * Returns the same VO with score filled in.
     */
    MatchCandidateVO score(String userId, String candidateUserId);

    /**
     * Phase 2b: build the category-specific snippet string shown on discovery card.
     * e.g. for matrimonial: "Vegetarian · MBA · 12 LPA · Mumbai"
     *      for travel:      "Backpacker · 3 trips/yr · Europe fan"
     */
    String buildSnippet(String candidateUserId);
}
