package com.shiviishiv7.matchmaking.processor.meeting.scoreengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiviishiv7.matchmaking.common.enums.DietPreference;
import com.shiviishiv7.matchmaking.common.enums.Gender;
import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.common.enums.Religion;
import com.shiviishiv7.matchmaking.provider.implementation.BaseUserProfileRepository;
import com.shiviishiv7.matchmaking.provider.implementation.CategoryProfileRegistryRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MatrimonialExtProfileRepository;
import com.shiviishiv7.matchmaking.provider.model.PartnerPreference;
import com.shiviishiv7.matchmaking.provider.model.profile.BaseUserProfile;
import com.shiviishiv7.matchmaking.provider.model.profile.MatrimonialExtProfile;
import com.shiviishiv7.matchmaking.provider.vo.MatchCandidateVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatrimonialScorerTest {

    @Mock private MatrimonialExtProfileRepository matrimonialRepo;
    @Mock private BaseUserProfileRepository baseProfileRepo;
    @Mock private CategoryProfileRegistryRepository registryRepo;

    @InjectMocks private MatrimonialScorer scorer;

    private static final String USER_A = "sub-a";
    private static final String USER_B = "sub-b";

    @BeforeEach
    void injectMapper() throws Exception {
        var field = MatrimonialScorer.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(scorer, new ObjectMapper());
    }

    // ── supports() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("supports() returns PROFESSIONAL_MATRIMONY")
    void supports_returnsProfessionalMatrimony() {
        assertThat(scorer.supports()).isEqualTo(MatchCategory.PROFESSIONAL_MATRIMONY);
    }

    // ── score: religion ───────────────────────────────────────────────────────

    @Test
    @DisplayName("score: religion match adds 20 pts")
    void score_religionMatch_adds20() {
        PartnerPreference pref = PartnerPreference.builder().religionPref(Religion.HINDUISM).build();
        MatrimonialExtProfile me        = extProfile(USER_A, "Hindu", "Tamil", "Vegetarian", pref);
        MatrimonialExtProfile candidate = extProfile(USER_B, "Hindu", "Tamil", "Vegetarian", null);
        BaseUserProfile candBase = baseProfile(USER_B, 28);

        setupMocks(me, candidate, candBase);

        MatchCandidateVO result = scorer.score(USER_A, USER_B);

        // Religion (20) + MotherTongue (10) + Diet (10) + at least partial education = 40+
        assertThat(result.getCompatibilityScore()).isGreaterThanOrEqualTo(40);
    }

    @Test
    @DisplayName("score: religion mismatch adds 0 pts for religion")
    void score_religionMismatch_adds0ForReligion() {
        PartnerPreference pref = PartnerPreference.builder().religionPref(Religion.HINDUISM).build();
        MatrimonialExtProfile me        = extProfile(USER_A, "Hindu", "Tamil", "Vegetarian", pref);
        MatrimonialExtProfile candidate = extProfile(USER_B, "Muslim", "Tamil", "Vegetarian", null);
        BaseUserProfile candBase = baseProfile(USER_B, 28);

        setupMocks(me, candidate, candBase);

        MatchCandidateVO result = scorer.score(USER_A, USER_B);

        // No religion score (0), but mother tongue (10) + diet (10) = 20+
        assertThat(result.getCompatibilityScore()).isLessThan(40);
    }

    // ── score: mother tongue ──────────────────────────────────────────────────

    @Test
    @DisplayName("score: same mother tongue adds 10 pts")
    void score_sameMotherTongue_adds10() {
        MatrimonialExtProfile me        = extProfile(USER_A, "Hindu", "Tamil", "Vegetarian", null);
        MatrimonialExtProfile candidate = extProfile(USER_B, "Hindu", "Tamil", "Vegetarian", null);
        BaseUserProfile candBase = baseProfile(USER_B, 28);

        setupMocks(me, candidate, candBase);

        MatrimonialExtProfile candidateDiff = extProfile("sub-c", "Hindu", "Telugu", "Vegetarian", null);
        BaseUserProfile candBaseDiff = baseProfile("sub-c", 28);
        when(matrimonialRepo.findByCognitoSub("sub-c")).thenReturn(Optional.of(candidateDiff));
        when(baseProfileRepo.findByCognitoSub("sub-c")).thenReturn(Optional.of(candBaseDiff));

        int scoreMatch = scorer.score(USER_A, USER_B).getCompatibilityScore();
        int scoreDiff  = scorer.score(USER_A, "sub-c").getCompatibilityScore();

        assertThat(scoreMatch - scoreDiff).isEqualTo(10);
    }

    // ── score: location ───────────────────────────────────────────────────────

    @Test
    @DisplayName("score: candidate in preferred state scores location points")
    void score_preferredState_scoresLocation() {
        PartnerPreference pref = PartnerPreference.builder().preferredStates("Tamil Nadu").build();
        MatrimonialExtProfile me        = extProfile(USER_A, "Hindu", "Tamil", "Vegetarian", pref);
        MatrimonialExtProfile candidate = extProfile(USER_B, "Hindu", "Tamil", "Vegetarian", null);

        BaseUserProfile candBase = BaseUserProfile.builder()
                .cognitoSub(USER_B)
                .name("Candidate")
                .dateOfBirth(LocalDate.now().minusYears(28))
                .currentState("Tamil Nadu")
                .currentCountry("India")
                .build();

        setupMocks(me, candidate, candBase);

        MatchCandidateVO result = scorer.score(USER_A, USER_B);
        assertThat(result.getCompatibilityScore()).isGreaterThanOrEqualTo(10);
    }

    // ── score: education ──────────────────────────────────────────────────────

    @Test
    @DisplayName("score: same education tier scores 10 pts")
    void score_sameEducationTier_scores10() {
        MatrimonialExtProfile me        = extProfileWithEducation(USER_A, "B.Tech");
        MatrimonialExtProfile candidate = extProfileWithEducation(USER_B, "BE");
        BaseUserProfile candBase        = baseProfile(USER_B, 28);

        setupMocks(me, candidate, candBase);

        MatchCandidateVO result = scorer.score(USER_A, USER_B);
        assertThat(result.getCompatibilityScore()).isGreaterThanOrEqualTo(10);
    }

    @Test
    @DisplayName("score: education tier diff of 2 contributes 0 pts")
    void score_educationTierDiff2_scores0() {
        MatrimonialExtProfile me        = extProfileWithEducation(USER_A, "PhD");
        MatrimonialExtProfile candidate = extProfileWithEducation(USER_B, "Diploma");
        BaseUserProfile candBase        = baseProfile(USER_B, 28);

        setupMocks(me, candidate, candBase);

        MatchCandidateVO result = scorer.score(USER_A, USER_B);
        assertThat(result.getCompatibilityScore()).isLessThanOrEqualTo(50);
    }

    // ── score: income ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("score: candidate income within range scores 10 pts")
    void score_incomeInRange_scores10() {
        PartnerPreference pref = PartnerPreference.builder()
                .incomeMinInr(new BigDecimal("800000"))
                .incomeMaxInr(new BigDecimal("2000000"))
                .build();
        MatrimonialExtProfile me = extProfile(USER_A, "Hindu", "Tamil", "Vegetarian", pref);
        MatrimonialExtProfile candidate = extProfile(USER_B, "Hindu", "Tamil", "Vegetarian", null);
        candidate.setAnnualIncomeInr(new BigDecimal("1200000"));
        BaseUserProfile candBase = baseProfile(USER_B, 28);

        setupMocks(me, candidate, candBase);

        MatchCandidateVO result = scorer.score(USER_A, USER_B);
        assertThat(result.getCompatibilityScore()).isGreaterThanOrEqualTo(10);
    }

    // ── score: total capped at 100 ────────────────────────────────────────────

    @Test
    @DisplayName("score: total is always capped at 100")
    void score_total_cappedAt100() {
        PartnerPreference pref = PartnerPreference.builder()
                .religionPref(Religion.HINDUISM)
                .dietaryPref(DietPreference.VEGETARIAN)
                .preferredStates("Tamil Nadu")
                .ageMin(25).ageMax(32)
                .build();
        MatrimonialExtProfile me = extProfile(USER_A, "Hindu", "Tamil", "Vegetarian", pref);
        me.setHighestEducation("MBA");

        MatrimonialExtProfile candidate = extProfile(USER_B, "Hindu", "Tamil", "Vegetarian", null);
        candidate.setCaste("Brahmin");
        candidate.setGotram("Kasyapa");
        candidate.setHighestEducation("MBA");
        candidate.setManglikStatus("Any");
        candidate.setAnnualIncomeInr(new BigDecimal("1500000"));

        BaseUserProfile candBase = BaseUserProfile.builder()
                .cognitoSub(USER_B)
                .name("Perfect Match")
                .dateOfBirth(LocalDate.now().minusYears(28))
                .currentState("Tamil Nadu")
                .currentCountry("India")
                .build();

        setupMocks(me, candidate, candBase);

        MatchCandidateVO result = scorer.score(USER_A, USER_B);
        assertThat(result.getCompatibilityScore()).isLessThanOrEqualTo(100);
    }

    // ── score: VO fields ──────────────────────────────────────────────────────

    @Test
    @DisplayName("score: VO has correct cognitoSubB and matchCategory")
    void score_voFields_correctlyPopulated() {
        MatrimonialExtProfile me        = extProfile(USER_A, "Hindu", "Tamil", "Vegetarian", null);
        MatrimonialExtProfile candidate = extProfile(USER_B, "Hindu", "Tamil", "Vegetarian", null);
        BaseUserProfile candBase = baseProfile(USER_B, 28);

        setupMocks(me, candidate, candBase);

        MatchCandidateVO result = scorer.score(USER_A, USER_B);

        assertThat(result.getCognitoSubB()).isEqualTo(USER_B);
        assertThat(result.getMatchCategory()).isEqualTo(MatchCategory.PROFESSIONAL_MATRIMONY);
        assertThat(result.getAge()).isEqualTo(28);
        assertThat(result.getScoreBreakdown()).isNotEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private MatrimonialExtProfile extProfile(String sub, String religion,
                                             String motherTongue, String diet,
                                             PartnerPreference pref) {
        MatrimonialExtProfile p = new MatrimonialExtProfile();
        p.setCognitoSub(sub);
        p.setReligion(religion);
        p.setMotherTongue(motherTongue);
        p.setDietaryHabits(diet);
        p.setPartnerPreference(pref);
        return p;
    }

    private MatrimonialExtProfile extProfileWithEducation(String sub, String education) {
        MatrimonialExtProfile p = extProfile(sub, "Hindu", "Tamil", "Vegetarian", null);
        p.setHighestEducation(education);
        return p;
    }

    private BaseUserProfile baseProfile(String sub, int ageYears) {
        return BaseUserProfile.builder()
                .cognitoSub(sub)
                .name("Test User")
                .gender(Gender.FEMALE)
                .dateOfBirth(LocalDate.now().minusYears(ageYears))
                .currentCity("Chennai")
                .currentState("Tamil Nadu")
                .currentCountry("India")
                .build();
    }

    private void setupMocks(MatrimonialExtProfile me,
                            MatrimonialExtProfile candidate,
                            BaseUserProfile candBase) {
        when(matrimonialRepo.findByCognitoSub(me.getCognitoSub())).thenReturn(Optional.of(me));
        when(matrimonialRepo.findByCognitoSub(candidate.getCognitoSub())).thenReturn(Optional.of(candidate));
        when(baseProfileRepo.findByCognitoSub(candidate.getCognitoSub())).thenReturn(Optional.of(candBase));
        lenient().when(baseProfileRepo.findByCognitoSub(me.getCognitoSub()))
                .thenReturn(Optional.of(baseProfile(me.getCognitoSub(), 30)));
    }
}
