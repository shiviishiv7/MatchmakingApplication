package com.shiviishiv7.matchmaking.ws;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.controller.ws.MatchmakingWebSocketController;
import com.shiviishiv7.matchmaking.provider.implementation.BaseUserProfileRepository;
import com.shiviishiv7.matchmaking.provider.implementation.CategoryProfileRegistryRepository;
import com.shiviishiv7.matchmaking.provider.model.profile.BaseUserProfile;
import com.shiviishiv7.matchmaking.provider.vo.ws.InstantSearchFilterVO;
import com.shiviishiv7.matchmaking.provider.vo.ws.PoolUserVO;
import com.shiviishiv7.matchmaking.provider.vo.ws.WebRTCSignalVO;
import com.shiviishiv7.matchmaking.service.pool.UserPoolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MatchmakingWebSocketController —
 * covers instant-match signaling: join, filter search, busy,
 * connection request (busy guard), and answer (busy marking).
 */
@ExtendWith(MockitoExtension.class)
class MatchmakingWebSocketControllerTest {

    @Mock private SimpMessagingTemplate           messaging;
    @Mock private BaseUserProfileRepository       profileRepo;
    @Mock private CategoryProfileRegistryRepository registryRepo;
    @Mock private UserPoolService                 poolService;
    @Mock private SimpMessageHeaderAccessor       accessor;

    @InjectMocks
    private MatchmakingWebSocketController controller;

    private static final String SUB_A = "sub-a";
    private static final String SUB_B = "sub-b";

    @BeforeEach
    void setUp() {
        Principal principal = () -> SUB_A;
        when(accessor.getUser()).thenReturn(principal);
    }

    // ─── webrtc.join ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-W01: join — new user added to pool, pool list sent back")
    void join_newUser_addedAndListSent() throws Exception {
        when(poolService.isInPool(SUB_A)).thenReturn(false);
        when(profileRepo.findByCognitoSub(SUB_A)).thenReturn(Optional.of(stubProfile(SUB_A)));
        when(registryRepo.findByCognitoSubAndIsActive(SUB_A, true)).thenReturn(List.of());
        when(poolService.getOtherUsers(SUB_A)).thenReturn(List.of());
        when(poolService.findPendingMatches(any())).thenReturn(java.util.Collections.emptyMap());

        controller.userJoin(accessor);

        verify(poolService).addUser(any(PoolUserVO.class));
        verify(messaging).convertAndSendToUser(eq(SUB_A), eq("/queue/webrtc"), any());
    }

    @Test
    @DisplayName("TC-W02: join — already-in-pool user is NOT re-added but still gets pool list")
    void join_existingUser_skipsAddButSendsList() throws Exception {
        when(poolService.isInPool(SUB_A)).thenReturn(true);
        when(poolService.getOtherUsers(SUB_A)).thenReturn(List.of());

        controller.userJoin(accessor);

        verify(poolService, never()).addUser(any());
        verify(messaging).convertAndSendToUser(eq(SUB_A), eq("/queue/webrtc"), any());
    }

    @Test
    @DisplayName("TC-W03: join — pool users returned are marked as seen")
    void join_returnedUsers_markedAsSeen() throws Exception {
        PoolUserVO bob = stubPoolUser(SUB_B);
        when(poolService.isInPool(SUB_A)).thenReturn(false);
        when(profileRepo.findByCognitoSub(SUB_A)).thenReturn(Optional.of(stubProfile(SUB_A)));
        when(registryRepo.findByCognitoSubAndIsActive(SUB_A, true)).thenReturn(List.of());
        when(poolService.getOtherUsers(SUB_A)).thenReturn(List.of(bob));
        when(poolService.findPendingMatches(any())).thenReturn(java.util.Collections.emptyMap());

        controller.userJoin(accessor);

        verify(poolService).markAsSeen(eq(SUB_A), eq(List.of(bob)));
    }

    // ─── webrtc.search ────────────────────────────────────────────────────────

    @Test
    @DisplayName("TC-W04: filter search — results found → sends pool list and removes pending request")
    void filterSearch_resultsFound_sendsPoolList() {
        PoolUserVO bob = stubPoolUser(SUB_B);
        InstantSearchFilterVO filter = new InstantSearchFilterVO();
        when(poolService.getFilteredUsers(SUB_A, filter)).thenReturn(List.of(bob));

        controller.filterSearch(filter, accessor);

        verify(messaging).convertAndSendToUser(eq(SUB_A), eq("/queue/webrtc"), eq(List.of(bob)));
        verify(poolService).markAsSeen(eq(SUB_A), eq(List.of(bob)));
        verify(poolService).removePendingRequest(SUB_A);
    }

    @Test
    @DisplayName("TC-W05: filter search — no results → saves pending request and sends NO_MATCH_NOW")
    void filterSearch_noResults_savesPendingAndSendsSignal() {
        InstantSearchFilterVO filter = new InstantSearchFilterVO();
        when(poolService.getFilteredUsers(SUB_A, filter)).thenReturn(List.of());

        controller.filterSearch(filter, accessor);

        verify(poolService).addPendingRequest(SUB_A, filter);
        ArgumentCaptor<WebRTCSignalVO> captor = ArgumentCaptor.forClass(WebRTCSignalVO.class);
        verify(messaging).convertAndSendToUser(eq(SUB_A), eq("/queue/webrtc"), captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("NO_MATCH_NOW");
    }

    // ─── webrtc.request — busy guard ──────────────────────────────────────────

    @Test
    @DisplayName("TC-W06: connection request — target busy → sends BUSY back to caller, does not forward")
    void connectionRequest_targetBusy_sendsBusyToCaller() {
        WebRTCSignalVO signal = new WebRTCSignalVO();
        signal.setToUserId(SUB_B);
        when(poolService.isBusy(SUB_B)).thenReturn(true);

        controller.connectionRequest(signal, accessor);

        ArgumentCaptor<WebRTCSignalVO> captor = ArgumentCaptor.forClass(WebRTCSignalVO.class);
        verify(messaging).convertAndSendToUser(eq(SUB_A), eq("/queue/webrtc"), captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("BUSY");
        assertThat(captor.getValue().getFromUserId()).isEqualTo(SUB_B);

        // Should NOT have forwarded to target
        verify(messaging, never()).convertAndSendToUser(eq(SUB_B), anyString(), any());
    }

    @Test
    @DisplayName("TC-W07: connection request — target available → forwards CONNECTION_REQUEST to target")
    void connectionRequest_targetAvailable_forwards() {
        WebRTCSignalVO signal = new WebRTCSignalVO();
        signal.setToUserId(SUB_B);
        when(poolService.isBusy(SUB_B)).thenReturn(false);

        controller.connectionRequest(signal, accessor);

        ArgumentCaptor<WebRTCSignalVO> captor = ArgumentCaptor.forClass(WebRTCSignalVO.class);
        verify(messaging).convertAndSendToUser(eq(SUB_B), eq("/queue/webrtc"), captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("CONNECTION_REQUEST");
        assertThat(captor.getValue().getFromUserId()).isEqualTo(SUB_A);
    }

    // ─── webrtc.busy (callee self-reports) ────────────────────────────────────

    @Test
    @DisplayName("TC-W08: webrtc.busy — callee reports busy → BUSY signal forwarded to original caller")
    void calleeReportsBusy_busySignalForwardedToCaller() {
        WebRTCSignalVO signal = new WebRTCSignalVO();
        signal.setToUserId(SUB_B); // SUB_B is the original caller

        controller.connectionBusy(signal, accessor); // SUB_A is callee

        ArgumentCaptor<WebRTCSignalVO> captor = ArgumentCaptor.forClass(WebRTCSignalVO.class);
        verify(messaging).convertAndSendToUser(eq(SUB_B), eq("/queue/webrtc"), captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("BUSY");
        assertThat(captor.getValue().getFromUserId()).isEqualTo(SUB_A);
    }

    // ─── webrtc.answer — marks both users busy ────────────────────────────────

    @Test
    @DisplayName("TC-W09: answer — both caller and callee marked busy when call confirmed")
    void answer_marksBothUsersBusy() {
        WebRTCSignalVO signal = new WebRTCSignalVO();
        signal.setToUserId(SUB_B); // SUB_A is callee, SUB_B is caller

        controller.handleAnswer(signal, accessor);

        verify(poolService).markBusy(SUB_A);
        verify(poolService).markBusy(SUB_B);
    }

    @Test
    @DisplayName("TC-W10: answer — ANSWER signal forwarded to caller")
    void answer_forwardedToCaller() {
        WebRTCSignalVO signal = new WebRTCSignalVO();
        signal.setToUserId(SUB_B);

        controller.handleAnswer(signal, accessor);

        ArgumentCaptor<WebRTCSignalVO> captor = ArgumentCaptor.forClass(WebRTCSignalVO.class);
        verify(messaging).convertAndSendToUser(eq(SUB_B), eq("/queue/webrtc"), captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("ANSWER");
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private BaseUserProfile stubProfile(String sub) {
        BaseUserProfile p = new BaseUserProfile();
        p.setCognitoSub(sub);
        p.setName("User " + sub);
        p.setDateOfBirth(LocalDate.of(1995, 1, 1));
        return p;
    }

    private PoolUserVO stubPoolUser(String sub) {
        return PoolUserVO.builder().cognitoSub(sub).firstName(sub).lastName("")
                .matchCategories(List.of()).build();
    }
}
