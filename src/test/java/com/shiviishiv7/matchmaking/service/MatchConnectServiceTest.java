package com.shiviishiv7.matchmaking.service;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingType;
import com.shiviishiv7.matchmaking.provider.implementation.BaseUserProfileRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MatchResultRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingRepository;
import com.shiviishiv7.matchmaking.provider.model.MatchResult;
import com.shiviishiv7.matchmaking.provider.model.Meeting;
import com.shiviishiv7.matchmaking.provider.model.profile.BaseUserProfile;
import com.shiviishiv7.matchmaking.provider.vo.ws.MeetingNotificationVO;
import com.shiviishiv7.matchmaking.service.email.EmailService;
import com.shiviishiv7.matchmaking.service.match.MatchConnectService;
import com.shiviishiv7.matchmaking.service.pool.UserPoolService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchConnectServiceTest {

    @Mock private MatchResultRepository matchResultRepository;
    @Mock private MeetingRepository meetingRepository;
    @Mock private UserPoolService userPoolService;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private BaseUserProfileRepository userProfileRepository;
    @Mock private EmailService emailService;

    @InjectMocks private MatchConnectService service;

    private static final String USER_A = "sub-a";
    private static final String USER_B = "sub-b";

    // ── connectNextOnlineMatch ────────────────────────────────────────────────

    @Test
    @DisplayName("no PENDING matches → returns false, no meeting created")
    void connectNext_noPendingMatches_returnsFalse() {
        when(matchResultRepository.findPendingMatchesForUser(USER_A))
                .thenReturn(Collections.emptyList());

        boolean result = service.connectNextOnlineMatch(USER_A);

        assertThat(result).isFalse();
        verifyNoInteractions(meetingRepository, messagingTemplate);
    }

    @Test
    @DisplayName("candidate offline → skips, returns false")
    void connectNext_candidateOffline_returnsFalse() {
        MatchResult match = buildMatch(1, USER_A, USER_B);
        when(matchResultRepository.findPendingMatchesForUser(USER_A)).thenReturn(List.of(match));
        when(userPoolService.isInPool(USER_B)).thenReturn(false);

        assertThat(service.connectNextOnlineMatch(USER_A)).isFalse();
        verifyNoInteractions(meetingRepository);
    }

    @Test
    @DisplayName("candidate online but busy → skips, returns false")
    void connectNext_candidateBusy_returnsFalse() {
        MatchResult match = buildMatch(1, USER_A, USER_B);
        when(matchResultRepository.findPendingMatchesForUser(USER_A)).thenReturn(List.of(match));
        when(userPoolService.isInPool(USER_B)).thenReturn(true);
        when(userPoolService.isBusy(USER_B)).thenReturn(true);

        assertThat(service.connectNextOnlineMatch(USER_A)).isFalse();
        verifyNoInteractions(meetingRepository);
    }

    @Test
    @DisplayName("candidate online → creates INSTANT meeting with WAITING_ROOM status")
    void connectNext_candidateOnline_createsMeeting() {
        MatchResult match = buildMatch(1, USER_A, USER_B);
        when(matchResultRepository.findPendingMatchesForUser(USER_A)).thenReturn(List.of(match));
        when(userPoolService.isInPool(USER_B)).thenReturn(true);
        when(userPoolService.isBusy(USER_B)).thenReturn(false);
        when(meetingRepository.save(any())).thenAnswer(inv -> {
            Meeting m = inv.getArgument(0);
            m.setId(10);
            return m;
        });

        boolean result = service.connectNextOnlineMatch(USER_A);

        assertThat(result).isTrue();
        ArgumentCaptor<Meeting> captor = ArgumentCaptor.forClass(Meeting.class);
        verify(meetingRepository).save(captor.capture());
        Meeting saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(MeetingStatus.WAITING_ROOM);
        assertThat(saved.getMeetingType()).isEqualTo(MeetingType.INSTANT);
        assertThat(saved.getMatchResultId()).isEqualTo(1);
    }

    @Test
    @DisplayName("candidate online → match status transitions to MEETING_SCHEDULED")
    void connectNext_candidateOnline_updatesMatchStatus() {
        MatchResult match = buildMatch(1, USER_A, USER_B);
        when(matchResultRepository.findPendingMatchesForUser(USER_A)).thenReturn(List.of(match));
        when(userPoolService.isInPool(USER_B)).thenReturn(true);
        when(userPoolService.isBusy(USER_B)).thenReturn(false);
        when(meetingRepository.save(any())).thenAnswer(inv -> { Meeting m = inv.getArgument(0); m.setId(10); return m; });

        service.connectNextOnlineMatch(USER_A);

        assertThat(match.getStatus()).isEqualTo(MatchStatus.MEETING_SCHEDULED);
        verify(matchResultRepository).save(match);
    }

    @Test
    @DisplayName("candidate online → WAITING_ROOM notification pushed to both users")
    void connectNext_candidateOnline_notifiesBothUsers() {
        MatchResult match = buildMatch(5, USER_A, USER_B);
        when(matchResultRepository.findPendingMatchesForUser(USER_A)).thenReturn(List.of(match));
        when(userPoolService.isInPool(USER_B)).thenReturn(true);
        when(userPoolService.isBusy(USER_B)).thenReturn(false);
        when(meetingRepository.save(any())).thenAnswer(inv -> { Meeting m = inv.getArgument(0); m.setId(20); return m; });

        service.connectNextOnlineMatch(USER_A);

        verify(messagingTemplate).convertAndSendToUser(eq(USER_A), eq("/queue/meeting"), any(MeetingNotificationVO.class));
        verify(messagingTemplate).convertAndSendToUser(eq(USER_B), eq("/queue/meeting"), any(MeetingNotificationVO.class));
    }

    @Test
    @DisplayName("first candidate offline, second online → connects second candidate")
    void connectNext_firstOfflineSecondOnline_connectsSecond() {
        MatchResult m1 = buildMatch(1, USER_A, "sub-c"); // offline
        MatchResult m2 = buildMatch(2, USER_A, USER_B);   // online
        when(matchResultRepository.findPendingMatchesForUser(USER_A)).thenReturn(List.of(m1, m2));
        when(userPoolService.isInPool("sub-c")).thenReturn(false);
        when(userPoolService.isInPool(USER_B)).thenReturn(true);
        when(userPoolService.isBusy(USER_B)).thenReturn(false);
        when(meetingRepository.save(any())).thenAnswer(inv -> { Meeting m = inv.getArgument(0); m.setId(30); return m; });

        boolean result = service.connectNextOnlineMatch(USER_A);

        assertThat(result).isTrue();
        verify(meetingRepository).save(argThat(m -> m.getMatchResultId() == 2));
    }

    // ── notifyWaitingMatchersOnJoin ───────────────────────────────────────────

    @Test
    @DisplayName("no waiting matchers → nothing happens when user joins pool")
    void notifyOnJoin_noWaiting_doesNothing() {
        when(matchResultRepository.findPendingByCandidateSub(USER_B))
                .thenReturn(Collections.emptyList());

        service.notifyWaitingMatchersOnJoin(USER_B);

        verifyNoInteractions(meetingRepository, messagingTemplate);
    }

    @Test
    @DisplayName("waiting user is offline → not connected when candidate joins")
    void notifyOnJoin_waiterOffline_notConnected() {
        MatchResult match = buildMatch(1, USER_A, USER_B);
        when(matchResultRepository.findPendingByCandidateSub(USER_B)).thenReturn(List.of(match));
        when(userPoolService.isInPool(USER_A)).thenReturn(false);

        service.notifyWaitingMatchersOnJoin(USER_B);

        verifyNoInteractions(meetingRepository, messagingTemplate);
    }

    @Test
    @DisplayName("waiting user is online → connected when candidate joins pool")
    void notifyOnJoin_waiterOnline_connects() {
        MatchResult match = buildMatch(1, USER_A, USER_B);
        when(matchResultRepository.findPendingByCandidateSub(USER_B)).thenReturn(List.of(match));
        when(userPoolService.isInPool(USER_A)).thenReturn(true);
        // For the connectNextOnlineMatch call that follows
        when(matchResultRepository.findPendingMatchesForUser(USER_A)).thenReturn(List.of(match));
        when(userPoolService.isInPool(USER_B)).thenReturn(true);
        when(userPoolService.isBusy(USER_B)).thenReturn(false);
        when(meetingRepository.save(any())).thenAnswer(inv -> { Meeting m = inv.getArgument(0); m.setId(40); return m; });

        service.notifyWaitingMatchersOnJoin(USER_B);

        // Should have pushed MATCH_NOW_ONLINE notification to the waiter
        verify(messagingTemplate).convertAndSendToUser(eq(USER_A), eq("/queue/matches"), any());
    }

    // ── sendNoOnlineMatchEmails ───────────────────────────────────────────────

    @Test
    @DisplayName("emails both users when no online match found")
    void sendEmails_bothProfilesExist_emailsBoth() {
        BaseUserProfile profileA = buildProfile(USER_A, "Alice", "alice@test.com");
        BaseUserProfile profileB = buildProfile(USER_B, "Bob",   "bob@test.com");
        when(userProfileRepository.findByCognitoSub(USER_A)).thenReturn(Optional.of(profileA));
        when(userProfileRepository.findByCognitoSub(USER_B)).thenReturn(Optional.of(profileB));

        service.sendNoOnlineMatchEmails(USER_A, USER_B);

        verify(emailService).sendMatchSavedEmail("alice@test.com", "Alice", "Bob");
        verify(emailService).sendMatchWaitingEmail("bob@test.com", "Bob", "Alice");
    }

    @Test
    @DisplayName("no email sent when a profile is missing")
    void sendEmails_missingProfile_noEmail() {
        when(userProfileRepository.findByCognitoSub(USER_A)).thenReturn(Optional.empty());
        when(userProfileRepository.findByCognitoSub(USER_B)).thenReturn(Optional.empty());

        service.sendNoOnlineMatchEmails(USER_A, USER_B);

        verifyNoInteractions(emailService);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private MatchResult buildMatch(int id, String subA, String subB) {
        return MatchResult.builder()
                .id(id)
                .cognitoSubA(subA)
                .cognitoSubB(subB)
                .matchCategory(MatchCategory.CASUAL_DATING)
                .status(MatchStatus.PENDING)
                .roundCount(0)
                .build();
    }

    private BaseUserProfile buildProfile(String sub, String name, String email) {
        return BaseUserProfile.builder()
                .cognitoSub(sub)
                .name(name)
                .email(email)
                .build();
    }
}
