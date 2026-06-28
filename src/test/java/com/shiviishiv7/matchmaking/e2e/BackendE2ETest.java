package com.shiviishiv7.matchmaking.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiviishiv7.matchmaking.common.enums.FeedbackResponse;
import com.shiviishiv7.matchmaking.common.enums.IntentType;
import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingType;
import com.shiviishiv7.matchmaking.common.enums.PostStatus;
import com.shiviishiv7.matchmaking.provider.implementation.MatchResultRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingFeedbackRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingRepository;
import com.shiviishiv7.matchmaking.provider.implementation.UserPostRepository;
import com.shiviishiv7.matchmaking.provider.model.MatchResult;
import com.shiviishiv7.matchmaking.provider.model.Meeting;
import com.shiviishiv7.matchmaking.provider.model.MeetingFeedback;
import com.shiviishiv7.matchmaking.provider.model.UserPost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Backend end-to-end integration tests.
 *
 * Runs the full Spring Boot context against an H2 in-memory database.
 * External dependencies (Redis, WebSocket broker, email) are mocked.
 * Authentication uses the built-in bypass token so no JWT setup is needed.
 *
 * Auth: every request carries the internal bypass token which sets
 * cognitoSub = "SYSTEM_LAMBDA" in CurrentUserContext.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.physical_naming_strategy=" +
            "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl",
        "app.mail.enabled=false",
        "anthropic.api.key=test-key-not-used"
})
class BackendE2ETest {

    private static final String AUTH = "Backend-Internal-Secret-Key-123XYZ";
    private static final String ME   = "SYSTEM_LAMBDA"; // what the bypass sets as cognitoSub

    @Autowired MockMvc        mockMvc;
    @Autowired ObjectMapper   objectMapper;
    @Autowired MatchResultRepository     matchRepo;
    @Autowired MeetingRepository         meetingRepo;
    @Autowired MeetingFeedbackRepository feedbackRepo;
    @Autowired UserPostRepository        postRepo;

    @MockBean(name = "redisConnectionFactory") RedisConnectionFactory redisConnectionFactory;
    @MockBean ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;
    @MockBean SimpMessagingTemplate messagingTemplate;
    @MockBean com.shiviishiv7.matchmaking.service.zoom.ZoomService zoomService;

    @BeforeEach
    void clean() {
        feedbackRepo.deleteAll();
        meetingRepo.deleteAll();
        matchRepo.deleteAll();
        postRepo.deleteAll();
    }

    // ── E2E-01: Full feedback loop — both users say NO → match ENDED ─────────

    @Test
    @DisplayName("E2E-01: Submit feedback from both users (NO) → match transitions to ENDED")
    void e2e_bothSayNo_matchEnded() throws Exception {
        // Arrange: create match + completed meeting
        MatchResult match = saveMatch(ME, "sub-b", MatchStatus.AWAITING_FEEDBACK);
        Meeting meeting   = saveCompletedMeeting(match.getId());

        // Pre-seed user B's feedback directly (can't impersonate via API without a second token)
        feedbackRepo.save(MeetingFeedback.builder()
                .meetingId(meeting.getId().toString())
                .cognitoSub("sub-b")
                .response(FeedbackResponse.NO)
                .build());

        // Act: user A (ME) submits their feedback via the API
        mockMvc.perform(post("/api/v1/meetings/{id}/feedback", meeting.getId())
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("response", "NO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        // Assert: match is now ENDED
        MatchResult updated = matchRepo.findById(match.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(MatchStatus.ENDED);
    }

    // ── E2E-02: Both YES + rounds left → ANOTHER_ROUND + new meeting ─────────

    @Test
    @DisplayName("E2E-02: Both users say YES → match transitions to ANOTHER_ROUND, new meeting scheduled")
    void e2e_bothSayYes_anotherRound() throws Exception {
        MatchResult match = saveMatch(ME, "sub-b", MatchStatus.AWAITING_FEEDBACK);
        match.setRoundCount(1);
        match.setMaxRounds(3);
        matchRepo.save(match);
        Meeting meeting = saveCompletedMeeting(match.getId());

        // User B seeds YES
        feedbackRepo.save(MeetingFeedback.builder()
                .meetingId(meeting.getId().toString())
                .cognitoSub("sub-b")
                .response(FeedbackResponse.YES)
                .build());

        // User A submits YES via API
        mockMvc.perform(post("/api/v1/meetings/{id}/feedback", meeting.getId())
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("response", "YES")))
                .andExpect(status().isOk());

        MatchResult updated = matchRepo.findById(match.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(MatchStatus.ANOTHER_ROUND);
        assertThat(updated.getRoundCount()).isEqualTo(2);

        // A new meeting must have been scheduled
        long meetingCount = meetingRepo.findByMatchResultId(match.getId()).size();
        assertThat(meetingCount).isGreaterThanOrEqualTo(2);
    }

    // ── E2E-03: Duplicate feedback rejected ───────────────────────────────────

    @Test
    @DisplayName("E2E-03: Submitting feedback twice for the same meeting returns error")
    void e2e_duplicateFeedback_rejected() throws Exception {
        MatchResult match = saveMatch(ME, "sub-b", MatchStatus.AWAITING_FEEDBACK);
        Meeting meeting   = saveCompletedMeeting(match.getId());

        // First submission
        mockMvc.perform(post("/api/v1/meetings/{id}/feedback", meeting.getId())
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("response", "YES")))
                .andExpect(status().isOk());

        // Second submission — should be rejected
        mockMvc.perform(post("/api/v1/meetings/{id}/feedback", meeting.getId())
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("response", "NO")))
                .andExpect(status().is4xxClientError());
    }

    // ── E2E-04: Match lifecycle — create / end / query by status ─────────────

    @Test
    @DisplayName("E2E-04: GET /matches/{id} returns the match")
    void e2e_getMatch_returnsVO() throws Exception {
        MatchResult match = saveMatch(ME, "sub-b", MatchStatus.PENDING);

        mockMvc.perform(get("/api/v1/matches/{id}", match.getId())
                        .header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.cognitoSubA").value(ME));
    }

    @Test
    @DisplayName("E2E-05: PATCH /matches/{id}/end transitions match to ENDED")
    void e2e_endMatch_statusBecomesEnded() throws Exception {
        MatchResult match = saveMatch(ME, "sub-b", MatchStatus.MEETING_SCHEDULED);

        mockMvc.perform(patch("/api/v1/matches/{id}/end", match.getId())
                        .header("Authorization", AUTH))
                .andExpect(status().isOk());

        assertThat(matchRepo.findById(match.getId()).orElseThrow().getStatus())
                .isEqualTo(MatchStatus.ENDED);
    }

    @Test
    @DisplayName("E2E-06: GET /matches/status/ENDED returns matches in ENDED state")
    void e2e_getByStatus_returnsFilteredList() throws Exception {
        saveMatch(ME, "sub-b", MatchStatus.ENDED);
        saveMatch(ME, "sub-c", MatchStatus.PENDING);

        mockMvc.perform(get("/api/v1/matches/status/ENDED")
                        .header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].status").value("ENDED"));
    }

    // ── E2E-07: Meeting CRUD ───────────────────────────────────────────────────

    @Test
    @DisplayName("E2E-07: POST /meetings creates a meeting and returns 200")
    void e2e_createMeeting_returns200() throws Exception {
        MatchResult match = saveMatch(ME, "sub-b", MatchStatus.PENDING);

        String body = objectMapper.writeValueAsString(Map.of(
                "matchResultId",   match.getId(),
                "roundNumber",     1,
                "scheduledAt",     "2026-12-01T10:00:00",
                "meetingType",     "SCHEDULED",
                "status",          "SCHEDULED",
                "durationMinutes", 30
        ));

        mockMvc.perform(post("/api/v1/meetings")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roundNumber").value(1));
    }

    @Test
    @DisplayName("E2E-08: GET /meetings/{id} returns meeting details")
    void e2e_getMeeting_returns200() throws Exception {
        MatchResult match = saveMatch(ME, "sub-b", MatchStatus.MEETING_SCHEDULED);
        Meeting meeting   = saveCompletedMeeting(match.getId());

        mockMvc.perform(get("/api/v1/meetings/{id}", meeting.getId())
                        .header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchResultId").value(match.getId()));
    }

    @Test
    @DisplayName("E2E-09: GET /meetings/match/{matchId} returns all meetings for a match")
    void e2e_getMeetingsForMatch_returnsList() throws Exception {
        MatchResult match = saveMatch(ME, "sub-b", MatchStatus.MEETING_SCHEDULED);
        saveCompletedMeeting(match.getId());

        mockMvc.perform(get("/api/v1/meetings/match/{matchId}", match.getId())
                        .header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].matchResultId").value(match.getId()));
    }

    // ── E2E-10: Next match — no pending → graceful no-active response ─────────

    // E2E-10: removed — /matches/next endpoint removed (WebRTC replaced by Zoom)

    // ── E2E-11: 401 without auth token ────────────────────────────────────────

    @Test
    @DisplayName("E2E-11: Requests without Authorization header return 401")
    void e2e_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/matches/1"))
                .andExpect(status().isUnauthorized());
    }

    // ── E2E-12: Feedback for non-existent meeting returns 4xx ────────────────

    @Test
    @DisplayName("E2E-12: Feedback for non-existent meeting returns 4xx")
    void e2e_feedbackForMissingMeeting_returns4xx() throws Exception {
        mockMvc.perform(post("/api/v1/meetings/9999/feedback")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("response", "YES")))
                .andExpect(status().is4xxClientError());
    }

    // ── E2E-13: POST /posts/submit creates ACTIVE post with 30-day expiry ─────

    @Test
    @DisplayName("E2E-13: POST /posts/submit creates an ACTIVE post with expiresAt ~30 days")
    void e2e_submitPost_createsActivePost() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "intent",    "MATRIMONIAL",
                "postText",  "28M | Delhi | Software engineer looking for a sincere life partner. Values family and honesty.",
                "answers",   java.util.List.of()
        ));

        mockMvc.perform(post("/api/v1/posts/submit")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        UserPost saved = postRepo.findByCognitoSubOrderByCreatedAtDesc(ME).get(0);
        assertThat(saved.getIntent()).isEqualTo(IntentType.MATRIMONIAL);
        assertThat(saved.getStatus()).isEqualTo(PostStatus.ACTIVE);
        assertThat(saved.getMatchCount()).isEqualTo(0);
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(29));
    }

    // ── E2E-14: POST /posts/submit with DATING intent ─────────────────────────

    @Test
    @DisplayName("E2E-14: POST /posts/submit with DATING intent creates a dating post")
    void e2e_submitDatingPost_createsWithDatingIntent() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "intent",    "DATING",
                "postText",  "25F | Mumbai | Looking for a genuine connection. I enjoy hiking and coffee and good conversation.",
                "answers",   java.util.List.of()
        ));

        mockMvc.perform(post("/api/v1/posts/submit")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        UserPost saved = postRepo.findByCognitoSubOrderByCreatedAtDesc(ME).get(0);
        assertThat(saved.getIntent()).isEqualTo(IntentType.DATING);
        assertThat(saved.getStatus()).isEqualTo(PostStatus.ACTIVE);
    }

    // ── E2E-15: GET /posts/my returns user's posts ────────────────────────────

    @Test
    @DisplayName("E2E-15: GET /posts/my returns all posts for the authenticated user")
    void e2e_getMyPosts_returnsList() throws Exception {
        postRepo.save(buildPost(IntentType.MATRIMONIAL));
        postRepo.save(buildPost(IntentType.DATING));

        mockMvc.perform(get("/api/v1/posts/my")
                        .header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    // ── E2E-16: GET /posts/my/active returns only ACTIVE posts ───────────────

    @Test
    @DisplayName("E2E-16: GET /posts/my/active returns only ACTIVE posts")
    void e2e_getMyActivePosts_returnsOnlyActive() throws Exception {
        UserPost active = postRepo.save(buildPost(IntentType.MATRIMONIAL));
        UserPost closed = buildPost(IntentType.DATING);
        closed.setStatus(PostStatus.CLOSED);
        postRepo.save(closed);

        mockMvc.perform(get("/api/v1/posts/my/active")
                        .header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    }

    // ── E2E-17: DELETE /posts/{id} closes the post ────────────────────────────

    @Test
    @DisplayName("E2E-17: DELETE /posts/{id} closes the post (CLOSED status)")
    void e2e_deletePost_closesPost() throws Exception {
        UserPost post = postRepo.save(buildPost(IntentType.MATRIMONIAL));

        mockMvc.perform(delete("/api/v1/posts/{id}", post.getId())
                        .header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        UserPost updated = postRepo.findById(post.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PostStatus.CLOSED);
    }

    // ── E2E-18: Meeting GET includes Zoom fields ───────────────────────────────

    @Test
    @DisplayName("E2E-18: GET /meetings/{id} response includes Zoom fields")
    void e2e_getMeeting_includesZoomFields() throws Exception {
        MatchResult match = saveMatch(ME, "sub-b", MatchStatus.MEETING_SCHEDULED);
        Meeting meeting = meetingRepo.save(Meeting.builder()
                .matchResultId(match.getId())
                .roundNumber(1)
                .scheduledAt(LocalDateTime.now().plusHours(3))
                .durationMinutes(30)
                .meetingType(MeetingType.SCHEDULED)
                .status(MeetingStatus.SCHEDULED)
                .zoomMeetingId("123456789")
                .zoomJoinUrl("https://zoom.us/j/123456789")
                .zoomPassword("abc123")
                .build());

        mockMvc.perform(get("/api/v1/meetings/{id}", meeting.getId())
                        .header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.zoomMeetingId").value("123456789"))
                .andExpect(jsonPath("$.data.zoomJoinUrl").value("https://zoom.us/j/123456789"))
                .andExpect(jsonPath("$.data.zoomPassword").value("abc123"));
    }

    // ── E2E-19: POST /posts/analyze requires intent field ─────────────────────

    @Test
    @DisplayName("E2E-19: POST /posts/analyze without intent returns 4xx")
    void e2e_analyzeWithoutIntent_returns4xx() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "postText", "Some post text here."
        ));

        mockMvc.perform(post("/api/v1/posts/analyze")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UserPost buildPost(IntentType intent) {
        return UserPost.builder()
                .cognitoSub(ME)
                .intent(intent)
                .postText("Test post text for matching purposes only.")
                .answersJson("[]")
                .inferredCategory(intent.name())
                .status(PostStatus.ACTIVE)
                .matchCount(0)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .profileUpdated(false)
                .build();
    }

    private MatchResult saveMatch(String subA, String subB, MatchStatus status) {
        return matchRepo.save(MatchResult.builder()
                .cognitoSubA(subA)
                .cognitoSubB(subB)
                .matchCategory(MatchCategory.CASUAL_DATING)
                .status(status)
                .roundCount(0)
                .maxRounds(3)
                .build());
    }

    private Meeting saveCompletedMeeting(int matchId) {
        return meetingRepo.save(Meeting.builder()
                .matchResultId(matchId)
                .roundNumber(1)
                .scheduledAt(LocalDateTime.now().minusMinutes(35))
                .durationMinutes(30)
                .meetingType(MeetingType.SCHEDULED)
                .status(MeetingStatus.COMPLETED)
                .build());
    }

    private String json(String key, String value) throws Exception {
        return objectMapper.writeValueAsString(Map.of(key, value));
    }
}
